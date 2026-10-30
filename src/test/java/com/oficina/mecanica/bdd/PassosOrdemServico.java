package com.oficina.mecanica.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.DocumentosDeTeste;
import com.oficina.mecanica.TokensDeTeste;
import com.oficina.mecanica.infrastructure.FilasDeTeste;
import com.oficina.mecanica.infrastructure.FilasDeTeste.MensagemRecebida;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class PassosOrdemServico {

    private static final Duration LIMITE = Duration.ofSeconds(20);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired MeterRegistry registry;

    @Value("${jwt.secret}")
    String segredoJwt;

    private String funcionario;
    private UUID clienteId;
    private UUID veiculoId;
    private UUID servicoId;
    private UUID pecaId;
    private UUID osId;
    private UUID sagaId;
    private final UUID orcamentoId = UUID.randomUUID();
    private final UUID pagamentoId = UUID.randomUUID();
    private final Map<String, MensagemRecebida> ultimosComandos = new HashMap<>();
    private UUID avisoDePagamento;

    @Dado("um cliente com um veículo cadastrado")
    public void umClienteComUmVeiculoCadastrado() throws Exception {
        funcionario = TokensDeTeste.assinar(segredoJwt, UUID.randomUUID().toString(), "FUNCIONARIO");
        clienteId = idDe(enviar(post("/api/clientes"), Map.of("cpfCnpj", DocumentosDeTeste.cpfValido(),
            "nome", "Cliente do Cenário", "email", "cliente.cenario@example.com")));
        veiculoId = idDe(enviar(post("/api/veiculos"), Map.of("placa", DocumentosDeTeste.placaValida(),
            "marca", "Volkswagen", "modelo", "Polo", "anoFabricacao", 2021, "clienteId", clienteId)));
    }

    @E("o catálogo com um serviço de {bigdecimal} e uma peça de {bigdecimal} com {int} unidades em estoque")
    public void oCatalogo(BigDecimal precoServico, BigDecimal precoPeca, int estoque) throws Exception {
        servicoId = idDe(enviar(post("/api/servicos"), Map.of("nome", "Troca de pastilhas",
            "descricao", "Mão de obra", "precoUnitario", precoServico, "tempoEstimadoHoras", 1)));
        pecaId = idDe(enviar(post("/api/pecas"), Map.of("nome", "Pastilha de freio", "descricao", "Jogo dianteiro",
            "precoUnitario", precoPeca, "quantidadeEstoque", estoque, "estoqueMinimo", 1)));
    }

    @Quando("o atendente abre a ordem de serviço")
    public void oAtendenteAbreAOrdemDeServico() throws Exception {
        var resposta = enviar(post("/api/ordens"), Map.of("clienteId", clienteId, "veiculoId", veiculoId));
        osId = UUID.fromString(resposta.get("id").asText());
        sagaId = UUID.fromString(resposta.get("sagaId").asText());
    }

    @Entao("a execução recebe o pedido de diagnóstico")
    public void aExecucaoRecebeOPedidoDeDiagnostico() {
        comandoRecebido(FilasDeTeste.EXECUTION_COMANDOS, "IniciarDiagnostico");
    }

    @Quando("a execução confirma o recebimento do pedido de diagnóstico")
    public void aExecucaoConfirmaODiagnosticoNaFila() {
        responder("ExecucaoEnfileirada", "IniciarDiagnostico",
            Map.of("etapa", "DIAGNOSTICO", "enfileiradaEm", agora()));
    }

    @Quando("o mecânico inicia o diagnóstico")
    public void oMecanicoIniciaODiagnostico() {
        publicarEAguardar("DiagnosticoIniciado", null, Map.of("mecanicoId", UUID.randomUUID().toString(),
            "iniciadoEm", agora()));
    }

    @Entao("a ordem de serviço fica {string}")
    public void aOrdemDeServicoFica(String status) {
        await().atMost(LIMITE).untilAsserted(() ->
            assertThat(consultar(get("/api/ordens/{id}", osId)).get("status").asText()).isEqualTo(status));
    }

    @Quando("o mecânico inclui o serviço e {int} peças na ordem de serviço")
    public void oMecanicoIncluiItens(int quantidadePecas) throws Exception {
        enviar(post("/api/ordens/{id}/servicos", osId), Map.of("itemId", servicoId, "quantidade", 1));
        enviar(post("/api/ordens/{id}/pecas", osId), Map.of("itemId", pecaId, "quantidade", quantidadePecas));
    }

    @Quando("o mecânico conclui o diagnóstico")
    public void oMecanicoConcluiODiagnostico() {
        publicarEAguardar("DiagnosticoConcluido", null, Map.of("mecanicoId", UUID.randomUUID().toString(),
            "concluidoEm", agora(), "observacoes", "Pastilhas no limite de desgaste"));
    }

    @Entao("o faturamento recebe o pedido de orçamento com total de {bigdecimal}")
    public void oFaturamentoRecebeOPedidoDeOrcamento(BigDecimal total) {
        var pedido = comandoRecebido(FilasDeTeste.BILLING_COMANDOS, "GerarOrcamento");
        var soma = BigDecimal.ZERO;
        for (var item : pedido.envelope().path("dados").path("itens")) {
            soma = soma.add(item.path("precoUnitario").decimalValue()
                .multiply(BigDecimal.valueOf(item.path("quantidade").asInt())));
        }
        assertThat(soma).isEqualByComparingTo(total);
        assertThat(pedido.envelope().path("dados").path("cliente").path("email").asText())
            .isEqualTo("cliente.cenario@example.com");
    }

    @Quando("o orçamento é gerado")
    public void oOrcamentoEhGerado() {
        responder("OrcamentoGerado", "GerarOrcamento", Map.of("orcamentoId", orcamentoId.toString(),
            "total", 360.00, "validoAte", Instant.now().plus(Duration.ofDays(7)).toString()));
    }

    @Quando("o cliente aprova o orçamento")
    public void oClienteAprovaOOrcamento() {
        publicarEAguardar("OrcamentoAprovado", null, Map.of("orcamentoId", orcamentoId.toString(),
            "aprovadoEm", agora(), "canal", "CLIENTE", "linkPagamento", "https://pagamento.example/checkout"));
    }

    @Quando("o cliente reprova o orçamento")
    public void oClienteReprovaOOrcamento() {
        publicarEAguardar("OrcamentoReprovado", null, Map.of("orcamentoId", orcamentoId.toString(),
            "reprovadoEm", agora(), "canal", "LINK"));
    }

    @Quando("o pagamento é aprovado")
    public void oPagamentoEhAprovado() {
        avisoDePagamento = publicarEAguardar("PagamentoAprovado", null, Map.of("orcamentoId", orcamentoId.toString(),
            "pagamentoId", pagamentoId.toString(), "valor", 360.00, "aprovadoEm", agora(), "meio", "credit_card"));
    }

    @Quando("o mesmo aviso de pagamento aprovado chega novamente")
    public void oMesmoAvisoChegaNovamente() {
        var antes = duplicadas("PagamentoAprovado");
        FilasDeTeste.publicarEvento(avisoDePagamento, "PagamentoAprovado", sagaId, osId, null,
            Map.of("orcamentoId", orcamentoId.toString(), "pagamentoId", pagamentoId.toString(), "valor", 360.00,
                "aprovadoEm", agora(), "meio", "credit_card"));
        await().atMost(LIMITE).until(() -> duplicadas("PagamentoAprovado") > antes);
    }

    @Quando("o pagamento é recusado")
    public void oPagamentoEhRecusado() {
        publicarEAguardar("PagamentoRecusado", null, Map.of("orcamentoId", orcamentoId.toString(),
            "pagamentoId", pagamentoId.toString(), "motivo", "cc_rejected_other_reason"));
    }

    @Entao("a execução recebe o pedido de reparo")
    public void aExecucaoRecebeOPedidoDeReparo() {
        comandoRecebido(FilasDeTeste.EXECUTION_COMANDOS, "IniciarReparo");
    }

    @Entao("a execução recebe um único pedido de reparo")
    public void aExecucaoRecebeUmUnicoPedidoDeReparo() {
        comandoRecebido(FilasDeTeste.EXECUTION_COMANDOS, "IniciarReparo");
        assertThat(FilasDeTeste.comandosRecebidos(FilasDeTeste.EXECUTION_COMANDOS, osId, "IniciarReparo")).hasSize(1);
    }

    @Quando("a execução confirma o recebimento do pedido de reparo")
    public void aExecucaoConfirmaOReparoNaFila() {
        responder("ExecucaoEnfileirada", "IniciarReparo", Map.of("etapa", "REPARO", "enfileiradaEm", agora()));
    }

    @Quando("o mecânico inicia o reparo")
    public void oMecanicoIniciaOReparo() {
        publicarEAguardar("ReparoIniciado", null, Map.of("mecanicoId", UUID.randomUUID().toString(),
            "iniciadoEm", agora()));
    }

    @Quando("o mecânico finaliza o reparo")
    public void oMecanicoFinalizaOReparo() {
        publicarEAguardar("ExecucaoFinalizada", null, Map.of("mecanicoId", UUID.randomUUID().toString(),
            "finalizadaEm", agora()));
    }

    @Quando("o mecânico registra falha na execução")
    public void oMecanicoRegistraFalha() {
        publicarEAguardar("ExecucaoFalhou", null, Map.of("mecanicoId", UUID.randomUUID().toString(),
            "motivo", "Disco de freio trincado, peça indisponível", "ocorridaEm", agora()));
    }

    @Entao("o acompanhamento da saga indica {string}")
    public void oAcompanhamentoDaSagaIndica(String estado) {
        await().atMost(LIMITE).untilAsserted(() ->
            assertThat(consultar(get("/api/ordens/{id}/saga", osId)).get("estado").asText()).isEqualTo(estado));
    }

    @Quando("o atendente registra a entrega do veículo")
    public void oAtendenteRegistraAEntrega() throws Exception {
        enviar(post("/api/ordens/{id}/entregar", osId), Map.of());
    }

    @E("o histórico da ordem de serviço passa por {string}")
    public void oHistoricoPassaPor(String sequencia) throws Exception {
        var esperados = Arrays.stream(sequencia.split(",")).map(String::trim).toList();
        var obtidos = new ArrayList<String>();
        for (var registro : consultar(get("/api/ordens/{id}/historico", osId))) {
            var destino = registro.get("statusDestino").asText();
            if (obtidos.isEmpty() || !obtidos.get(obtidos.size() - 1).equals(destino)) {
                obtidos.add(destino);
            }
        }
        assertThat(obtidos).containsExactlyElementsOf(esperados);
    }

    @E("o estoque da peça fica com {int} unidades")
    public void oEstoqueDaPecaFica(int quantidade) {
        await().atMost(LIMITE).untilAsserted(() -> assertThat(
            consultar(get("/api/pecas/{id}", pecaId)).get("quantidadeEstoque").asInt()).isEqualTo(quantidade));
    }

    @Entao("a execução recebe o cancelamento")
    public void aExecucaoRecebeOCancelamento() {
        comandoRecebido(FilasDeTeste.EXECUTION_COMANDOS, "CancelarExecucao");
    }

    @Quando("a execução confirma o cancelamento")
    public void aExecucaoConfirmaOCancelamento() {
        responder("ExecucaoCancelada", "CancelarExecucao", Map.of("motivo", "compensação", "canceladaEm", agora()));
    }

    @Entao("o faturamento recebe o cancelamento do orçamento")
    public void oFaturamentoRecebeOCancelamentoDoOrcamento() {
        comandoRecebido(FilasDeTeste.BILLING_COMANDOS, "CancelarOrcamento");
    }

    @Quando("o faturamento confirma o cancelamento do orçamento")
    public void oFaturamentoConfirmaOCancelamentoDoOrcamento() {
        responder("OrcamentoCancelado", "CancelarOrcamento", Map.of("orcamentoId", orcamentoId.toString(),
            "motivo", "compensação"));
    }

    @Entao("o faturamento recebe o pedido de estorno do pagamento")
    public void oFaturamentoRecebeOPedidoDeEstorno() {
        comandoRecebido(FilasDeTeste.BILLING_COMANDOS, "EstornarPagamento");
    }

    @Quando("o faturamento confirma o estorno do pagamento")
    public void oFaturamentoConfirmaOEstorno() {
        responder("PagamentoEstornado", "EstornarPagamento", Map.of("orcamentoId", orcamentoId.toString(),
            "pagamentoId", pagamentoId.toString(), "valor", 240.00, "estornadoEm", agora()));
    }

    @Entao("a execução recebe o pedido de reabertura do diagnóstico")
    public void aExecucaoRecebeOPedidoDeReabertura() {
        var pedido = comandoRecebido(FilasDeTeste.EXECUTION_COMANDOS, "ReabrirDiagnostico");
        assertThat(pedido.envelope().path("dados").path("motivo").asText()).contains("sem serviços ou peças");
    }

    @E("o faturamento não recebe pedido de orçamento")
    public void oFaturamentoNaoRecebePedidoDeOrcamento() {
        assertThat(FilasDeTeste.comandosRecebidos(FilasDeTeste.BILLING_COMANDOS, osId, "GerarOrcamento")).isEmpty();
    }

    @Quando("a execução não confirma o recebimento dentro do prazo")
    public void aExecucaoNaoConfirmaDentroDoPrazo() {
        jdbc.update("UPDATE sagas_ordem_servico SET prazo_resposta = prazo_resposta - INTERVAL '1 hour' "
            + "WHERE os_id = ?", osId);
    }

    @Entao("a execução recebe novamente o pedido de diagnóstico, com nova identificação")
    public void aExecucaoRecebeNovamenteOPedido() {
        await().atMost(LIMITE).untilAsserted(() -> assertThat(
            FilasDeTeste.comandosRecebidos(FilasDeTeste.EXECUTION_COMANDOS, osId, "IniciarDiagnostico")).hasSize(2));
        var pedidos = FilasDeTeste.comandosRecebidos(FilasDeTeste.EXECUTION_COMANDOS, osId, "IniciarDiagnostico");
        assertThat(pedidos.get(1).mensagemId()).isNotEqualTo(pedidos.get(0).mensagemId());
        assertThat(pedidos.get(1).envelope().path("dados")).isEqualTo(pedidos.get(0).envelope().path("dados"));
        ultimosComandos.put("IniciarDiagnostico", pedidos.get(1));
    }

    @Dado("uma ordem de serviço em diagnóstico")
    public void umaOrdemEmDiagnostico() throws Exception {
        oAtendenteAbreAOrdemDeServico();
        aExecucaoRecebeOPedidoDeDiagnostico();
        aExecucaoConfirmaODiagnosticoNaFila();
        oMecanicoIniciaODiagnostico();
        aOrdemDeServicoFica("EM_DIAGNOSTICO");
    }

    @Dado("uma ordem de serviço com orçamento enviado ao cliente e {int} peças reservadas")
    public void umaOrdemComOrcamentoEnviado(int pecas) throws Exception {
        umaOrdemEmDiagnostico();
        enviar(post("/api/ordens/{id}/pecas", osId), Map.of("itemId", pecaId, "quantidade", pecas));
        oMecanicoConcluiODiagnostico();
        comandoRecebido(FilasDeTeste.BILLING_COMANDOS, "GerarOrcamento");
        oOrcamentoEhGerado();
        aOrdemDeServicoFica("AGUARDANDO_APROVACAO");
    }

    @Dado("uma ordem de serviço aguardando pagamento com {int} peças reservadas")
    public void umaOrdemAguardandoPagamento(int pecas) throws Exception {
        umaOrdemComOrcamentoEnviado(pecas);
        oClienteAprovaOOrcamento();
        aOrdemDeServicoFica("AGUARDANDO_PAGAMENTO");
    }

    @Dado("uma ordem de serviço em execução com {int} peças reservadas")
    public void umaOrdemEmExecucao(int pecas) throws Exception {
        umaOrdemAguardandoPagamento(pecas);
        oPagamentoEhAprovado();
        aExecucaoRecebeOPedidoDeReparo();
        aExecucaoConfirmaOReparoNaFila();
        oMecanicoIniciaOReparo();
        oAcompanhamentoDaSagaIndica("EM_EXECUCAO");
    }

    private MensagemRecebida comandoRecebido(String fila, String tipo) {
        var comando = FilasDeTeste.aguardarComando(fila, osId, tipo);
        var todos = FilasDeTeste.comandosRecebidos(fila, osId, tipo);
        var ultimo = todos.isEmpty() ? comando : todos.get(todos.size() - 1);
        ultimosComandos.put(tipo, ultimo);
        return ultimo;
    }

    private void responder(String evento, String comando, Map<String, Object> dados) {
        var pedido = ultimosComandos.get(comando);
        assertThat(pedido).as("pedido %s ainda não recebido", comando).isNotNull();
        publicarEAguardar(evento, pedido.mensagemId(), dados);
    }

    private UUID publicarEAguardar(String tipo, UUID causadaPor, Map<String, Object> dados) {
        var mensagemId = FilasDeTeste.publicarEvento(tipo, sagaId, osId, causadaPor, new LinkedHashMap<>(dados));
        await().atMost(LIMITE).until(() -> jdbc.queryForObject(
            "SELECT count(*) FROM saga_passos WHERE mensagem_id = ?", Integer.class, mensagemId) > 0);
        return mensagemId;
    }

    private double duplicadas(String tipo) {
        var contador = registry.find("oficina.mensageria.consumidas").tags("tipo", tipo, "resultado", "duplicada")
            .counter();
        return contador == null ? 0 : contador.count();
    }

    private JsonNode enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requisicao,
                            Map<String, Object> corpo) throws Exception {
        var resposta = mvc.perform(requisicao.header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
            .andReturn().getResponse();
        assertThat(resposta.getStatus()).as(resposta.getContentAsString()).isBetween(200, 299);
        var conteudo = resposta.getContentAsString();
        return conteudo.isBlank() ? json.createObjectNode() : json.readTree(conteudo);
    }

    private JsonNode consultar(RequestBuilder requisicao) throws Exception {
        var resposta = mvc.perform(withAuthorization(requisicao)).andReturn().getResponse();
        assertThat(resposta.getStatus()).as(resposta.getContentAsString()).isEqualTo(200);
        return json.readTree(resposta.getContentAsString());
    }

    private RequestBuilder withAuthorization(RequestBuilder requisicao) {
        return contexto -> {
            var montada = requisicao.buildRequest(contexto);
            montada.addHeader("Authorization", "Bearer " + funcionario);
            return montada;
        };
    }

    private static UUID idDe(JsonNode resposta) {
        return UUID.fromString(resposta.get("id").asText());
    }

    private static String agora() {
        return Instant.now().toString();
    }
}
