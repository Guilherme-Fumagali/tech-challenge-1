package com.oficina.mecanica.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oficina.mecanica.infrastructure.web.dto.request.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrdemServicoIntegrationTest extends IntegracaoBase {

    @Value("${jwt.secret}") String jwtSecret;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void deveAbrirOrdemEIncluirItensDuranteODiagnostico() throws Exception {
        var token = obterToken();

        var clienteResp = mvc.perform(post("/api/clientes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarClienteRequest(
                    "529.982.247-25", "João Silva", "joao@email.com", "11999999999"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        var clienteId = mapper.readTree(clienteResp).get("id").asText();

        var veiculoId = criarVeiculo(token, clienteId, "ABC1234");

        var servicoResp = mvc.perform(post("/api/servicos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarServicoRequest(
                    "Troca de óleo", "Óleo motor 5W30", new BigDecimal("120.00"), new BigDecimal("1.0")))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        var servicoId = mapper.readTree(servicoResp).get("id").asText();

        var pecaResp = mvc.perform(post("/api/pecas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarPecaRequest(
                    "Filtro de óleo", "Filtro Mann", new BigDecimal("45.00"), 20, 5))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        var pecaId = mapper.readTree(pecaResp).get("id").asText();

        var osId = criarOS(token, clienteId, veiculoId);
        mvc.perform(get("/api/ordens/{id}", osId).header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.status").value("RECEBIDA"));

        mvc.perform(post("/api/ordens/{id}/servicos", osId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AdicionarItemRequest(UUID.fromString(servicoId), 1))))
            .andExpect(status().isBadRequest());

        alterarStatus(osId, "EM_DIAGNOSTICO");

        mvc.perform(post("/api/ordens/{id}/servicos", osId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AdicionarItemRequest(UUID.fromString(servicoId), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itensServico", hasSize(1)));

        mvc.perform(post("/api/ordens/{id}/pecas", osId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AdicionarItemRequest(UUID.fromString(pecaId), 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itensPeca", hasSize(1)))
            .andExpect(jsonPath("$.orcamentoTotal").value(210.00));

        mvc.perform(get("/api/pecas/{id}", pecaId)
                .header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.quantidadeEstoque").value(18));

        mvc.perform(get("/api/ordens/{id}/status", osId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
    }

    @Test
    void entregaDoVeiculoEncerraAOrdemFinalizada() throws Exception {
        var token = obterToken();
        var clienteId = criarCliente(token, "11.222.333/0001-81");
        var osId = criarOS(token, clienteId, criarVeiculo(token, clienteId, "MER1C23"));

        mvc.perform(post("/api/ordens/{id}/entregar", osId).header("Authorization", "Bearer " + token))
            .andExpect(status().isUnprocessableEntity());

        alterarStatus(osId, "FINALIZADA");

        mvc.perform(post("/api/ordens/{id}/entregar", osId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void listagem_deveOrdenarPorPrioridadeEExcluirFinalizadas() throws Exception {
        var token = obterToken();
        var clienteId = criarCliente(token, "22.333.444/0001-81");
        var veiculoId = criarVeiculo(token, clienteId, "LST0001");

        var osRecebida = criarOS(token, clienteId, veiculoId);
        var osEmDiagnostico = criarOS(token, clienteId, veiculoId);
        alterarStatus(osEmDiagnostico, "EM_DIAGNOSTICO");
        var osAguardandoAprovacao = criarOS(token, clienteId, veiculoId);
        alterarStatus(osAguardandoAprovacao, "AGUARDANDO_APROVACAO");
        var osAguardandoPagamento = criarOS(token, clienteId, veiculoId);
        alterarStatus(osAguardandoPagamento, "AGUARDANDO_PAGAMENTO");
        var osEmExecucao = criarOS(token, clienteId, veiculoId);
        alterarStatus(osEmExecucao, "EM_EXECUCAO");
        var osFinalizada = criarOS(token, clienteId, veiculoId);
        alterarStatus(osFinalizada, "FINALIZADA");

        var listaResp = mvc.perform(get("/api/ordens").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        var idsRelevantes = java.util.List.of(osEmExecucao, osAguardandoPagamento, osAguardandoAprovacao,
            osEmDiagnostico, osRecebida);
        var ordemObtida = new java.util.ArrayList<String>();
        for (var node : mapper.readTree(listaResp)) {
            var id = node.get("id").asText();
            if (idsRelevantes.contains(id)) {
                ordemObtida.add(id);
            }
            assertThat(id).isNotEqualTo(osFinalizada);
        }

        assertThat(ordemObtida).containsExactly(osEmExecucao, osAguardandoPagamento, osAguardandoAprovacao,
            osEmDiagnostico, osRecebida);
    }

    @Test
    void clienteNaoAcessaRotasDeGestao() throws Exception {
        var tokenCliente = obterTokenCliente(UUID.randomUUID().toString());

        mvc.perform(post("/api/pecas")
                .header("Authorization", "Bearer " + tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/clientes").header("Authorization", "Bearer " + tokenCliente))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/ordens/relatorio/tempo-medio").header("Authorization", "Bearer " + tokenCliente))
            .andExpect(status().isForbidden());
    }

    @Test
    void clienteVeApenasAsPropriasOrdensEVeiculos() throws Exception {
        var funcionario = obterToken();
        var clienteA = criarCliente(funcionario, "77.888.999/0001-81");
        var clienteB = criarCliente(funcionario, "88.999.000/0001-98");
        var osA = criarOS(funcionario, clienteA, criarVeiculo(funcionario, clienteA, "CLA0001"));
        var veiculoB = criarVeiculo(funcionario, clienteB, "CLB0001");
        var osB = criarOS(funcionario, clienteB, veiculoB);
        var tokenA = obterTokenCliente(clienteA);

        mvc.perform(get("/api/ordens").header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].id", contains(osA)));

        mvc.perform(get("/api/ordens/{id}", osA).header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk());

        mvc.perform(get("/api/ordens/{id}", osB).header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isNotFound());

        mvc.perform(get("/api/veiculos/{id}", veiculoB).header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isNotFound());

        mvc.perform(get("/api/veiculos").header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].clienteId", everyItem(is(clienteA))));
    }

    @Test
    void migrationCadastraFuncionarioDeHomologacaoConfigurado() {
        var nome = jdbc.queryForObject(
            "SELECT nome FROM funcionarios WHERE cpf = ? AND status = 'ATIVO'", String.class, "39053344705");

        assertThat(nome).isEqualTo("Funcionária de Teste");
    }

    @Test
    void contratoOpenApiVersionadoDeveCorresponderAAplicacao() throws Exception {
        var resposta = mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        var ordenado = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();
        var contrato = (ObjectNode) ordenado.readTree(resposta);
        contrato.remove("servers");
        var gerado = ordenado.writeValueAsString(ordenado.convertValue(contrato, Object.class)) + "\n";

        var arquivo = Path.of("docs/openapi.json");
        if (Boolean.getBoolean("atualizarOpenApi")) {
            Files.writeString(arquivo, gerado, StandardCharsets.UTF_8);
        }

        assertThat(arquivo)
            .as("docs/openapi.json desatualizado: execute ./mvnw test -Dtest=OrdemServicoIntegrationTest -DatualizarOpenApi=true")
            .exists()
            .content(StandardCharsets.UTF_8).isEqualTo(gerado);
    }

    // ── Helpers ────────────────────────────────────────────

    @Test
    void parametroObrigatorioAusenteRetornaBadRequest() throws Exception {
        mvc.perform(get("/api/ordens/relatorio/tempo-medio")
                .header("Authorization", "Bearer " + obterToken()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void identificadorMalformadoRetornaBadRequest() throws Exception {
        mvc.perform(get("/api/ordens/nao-e-uuid")
                .header("Authorization", "Bearer " + obterToken()))
            .andExpect(status().isBadRequest());
    }

    private void alterarStatus(String osId, String status) {
        jdbc.update("UPDATE ordens_servico SET status = ? WHERE id = ?", status, UUID.fromString(osId));
    }

    /**
     * Forja um token com a mesma chave da aplicação.
     *
     * <p>Desde a Fase 3 quem emite é a Lambda de autenticação por CPF (ADR-003); a aplicação
     * só valida. O teste de integração da API, portanto, produz o token diretamente em vez
     * de chamar um endpoint de login que não existe mais.
     */
    private String obterToken() {
        return token(UUID.randomUUID().toString(), "FUNCIONARIO");
    }

    private String obterTokenCliente(String clienteId) {
        return token(clienteId, "CLIENTE");
    }

    private String token(String sub, String role) {
        var agora = Instant.now();
        var chave = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .subject(sub)
            .issuer("oficina-auth")
            .claim("cpf", "52998224725")
            .claim("nome", "Usuário de Teste")
            .claim("role", role)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusSeconds(900)))
            .signWith(chave)
            .compact();
    }

    private String criarCliente(String token, String cpfCnpj) throws Exception {
        var resp = mvc.perform(post("/api/clientes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarClienteRequest(
                    cpfCnpj, "Cliente Teste", null, null))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resp).get("id").asText();
    }

    private String criarVeiculo(String token, String clienteId, String placa) throws Exception {
        var resp = mvc.perform(post("/api/veiculos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarVeiculoRequest(
                    placa, "Honda", "Civic", 2020,
                    java.util.UUID.fromString(clienteId)))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resp).get("id").asText();
    }

    private String criarOS(String token, String clienteId, String veiculoId) throws Exception {
        var resp = mvc.perform(post("/api/ordens")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarOrdemServicoRequest(
                    java.util.UUID.fromString(clienteId),
                    java.util.UUID.fromString(veiculoId)))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resp).get("id").asText();
    }
}
