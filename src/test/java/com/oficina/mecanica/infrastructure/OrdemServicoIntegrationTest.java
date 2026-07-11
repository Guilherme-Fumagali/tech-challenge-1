package com.oficina.mecanica.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.infrastructure.web.dto.request.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OrdemServicoIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("oficina_test")
        .withUsername("oficina")
        .withPassword("oficina");

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired OrdemServicoRepository osRepository;

    @Test
    void deveExecutarFluxoCompletoDeUmaOrdemDeServico() throws Exception {
        var token = obterToken();

        // 1. Cadastrar cliente
        var clienteResp = mvc.perform(post("/api/clientes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarClienteRequest(
                    "529.982.247-25", "João Silva", "joao@email.com", "11999999999"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        var clienteId = mapper.readTree(clienteResp).get("id").asText();

        // 2. Cadastrar veículo
        var veiculoResp = mvc.perform(post("/api/veiculos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarVeiculoRequest(
                    "ABC1234", "Toyota", "Corolla", 2022,
                    java.util.UUID.fromString(clienteId)))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        var veiculoId = mapper.readTree(veiculoResp).get("id").asText();

        // 3. Cadastrar serviço
        var servicoResp = mvc.perform(post("/api/servicos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarServicoRequest(
                    "Troca de óleo", "Óleo motor 5W30", new BigDecimal("120.00"), new BigDecimal("1.0")))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        var servicoId = mapper.readTree(servicoResp).get("id").asText();

        // 4. Cadastrar peça com estoque
        var pecaResp = mvc.perform(post("/api/pecas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarPecaRequest(
                    "Filtro de óleo", "Filtro Mann", new BigDecimal("45.00"), 20, 5))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        var pecaId = mapper.readTree(pecaResp).get("id").asText();

        // 5. Abrir OS
        var osResp = mvc.perform(post("/api/ordens")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarOrdemServicoRequest(
                    java.util.UUID.fromString(clienteId),
                    java.util.UUID.fromString(veiculoId)))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RECEBIDA"))
            .andReturn().getResponse().getContentAsString();

        var osId = mapper.readTree(osResp).get("id").asText();

        // 6. Iniciar diagnóstico
        mvc.perform(post("/api/ordens/{id}/iniciar-diagnostico", osId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));

        // 7. Adicionar serviço (snapshot de preço capturado aqui)
        mvc.perform(post("/api/ordens/{id}/servicos", osId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AdicionarItemRequest(
                    java.util.UUID.fromString(servicoId), 1))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itensServico", hasSize(1)));

        // 8. Adicionar peça (decrementa estoque)
        mvc.perform(post("/api/ordens/{id}/pecas", osId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AdicionarItemRequest(
                    java.util.UUID.fromString(pecaId), 2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itensPeca", hasSize(1)));

        // Verificar que estoque foi decrementado: 20 - 2 = 18
        mvc.perform(get("/api/pecas/{id}", pecaId)
                .header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.quantidadeEstoque").value(18));

        // 9. Gerar orçamento: 120 + (2 * 45) = 210
        mvc.perform(post("/api/ordens/{id}/gerar-orcamento", osId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
            .andExpect(jsonPath("$.orcamentoTotal").value(210.00));

        // 10. Consulta pública de status (sem token)
        mvc.perform(get("/api/ordens/{id}/status", osId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));

        // 11. Aprovar orçamento
        mvc.perform(post("/api/ordens/{id}/aprovar", osId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
            .andExpect(jsonPath("$.dataInicio").isNotEmpty());

        // 12. Concluir serviços
        mvc.perform(post("/api/ordens/{id}/concluir", osId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FINALIZADA"))
            .andExpect(jsonPath("$.dataConclusao").isNotEmpty());

        // 13. Entregar veículo
        mvc.perform(post("/api/ordens/{id}/entregar", osId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void deveEstornarEstoqueAoCancelar() throws Exception {
        var token = obterToken();

        var clienteId = criarCliente(token, "11.222.333/0001-81");
        var veiculoId = criarVeiculo(token, clienteId, "MER1C23");
        var pecaId = criarPeca(token, 10);
        var osId = criarOS(token, clienteId, veiculoId);

        // Diagnóstico → adicionar peça (estoque: 10 → 7)
        mvc.perform(post("/api/ordens/{id}/iniciar-diagnostico", osId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/ordens/{id}/pecas", osId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AdicionarItemRequest(
                    java.util.UUID.fromString(pecaId), 3))))
            .andExpect(status().isOk());

        // Gerar orçamento → reprovar (estoque deve voltar: 7 → 10)
        mvc.perform(post("/api/ordens/{id}/gerar-orcamento", osId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/ordens/{id}/reprovar", osId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELADA"));

        mvc.perform(get("/api/pecas/{id}", pecaId)
                .header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.quantidadeEstoque").value(10));
    }

    @Test
    void listagem_deveOrdenarPorPrioridadeEExcluirFinalizadas() throws Exception {
        var token = obterToken();
        var clienteId = criarCliente(token, "22.333.444/0001-81");
        var veiculoId = criarVeiculo(token, clienteId, "LST0001");
        var pecaId = criarPeca(token, 10);

        var osRecebida = criarOS(token, clienteId, veiculoId);

        var osEmDiagnostico = criarOS(token, clienteId, veiculoId);
        avancarParaDiagnostico(token, osEmDiagnostico, pecaId);

        var osAguardandoAprovacao = criarOS(token, clienteId, veiculoId);
        avancarParaDiagnostico(token, osAguardandoAprovacao, pecaId);
        mvc.perform(post("/api/ordens/{id}/gerar-orcamento", osAguardandoAprovacao)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        var osEmExecucao = criarOS(token, clienteId, veiculoId);
        avancarParaDiagnostico(token, osEmExecucao, pecaId);
        mvc.perform(post("/api/ordens/{id}/gerar-orcamento", osEmExecucao)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/ordens/{id}/aprovar", osEmExecucao)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        var osFinalizada = criarOS(token, clienteId, veiculoId);
        avancarParaDiagnostico(token, osFinalizada, pecaId);
        mvc.perform(post("/api/ordens/{id}/gerar-orcamento", osFinalizada)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/ordens/{id}/aprovar", osFinalizada)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/ordens/{id}/concluir", osFinalizada)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        var listaResp = mvc.perform(get("/api/ordens").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // A tabela não é limpa entre testes desta classe (mesmo container Postgres) — filtra
        // apenas os IDs criados aqui e valida a ordem relativa entre eles, ignorando o restante.
        var idsRelevantes = java.util.List.of(osEmExecucao, osAguardandoAprovacao, osEmDiagnostico, osRecebida);
        var ordemObtida = new java.util.ArrayList<String>();
        for (var node : mapper.readTree(listaResp)) {
            var id = node.get("id").asText();
            if (idsRelevantes.contains(id)) {
                ordemObtida.add(id);
            }
            assertThat(id).isNotEqualTo(osFinalizada);
        }

        assertThat(ordemObtida).containsExactly(osEmExecucao, osAguardandoAprovacao, osEmDiagnostico, osRecebida);
    }

    @Test
    void aprovacaoExterna_deveAprovarComTokenValidoSemAutenticacao() throws Exception {
        var token = obterToken();
        var clienteId = criarCliente(token, "33.444.555/0001-81");
        var veiculoId = criarVeiculo(token, clienteId, "EXT0001");
        var pecaId = criarPeca(token, 10);
        var osId = criarOS(token, clienteId, veiculoId);

        avancarParaDiagnostico(token, osId, pecaId);
        mvc.perform(post("/api/ordens/{id}/gerar-orcamento", osId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        // Token lido diretamente do repositório — nunca exposto pela API, simula o link recebido por e-mail
        var tokenAprovacao = osRepository.buscarPorId(java.util.UUID.fromString(osId))
            .orElseThrow().getTokenAprovacaoExterna();

        mvc.perform(post("/api/ordens/{id}/aprovar-externo", osId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AprovarOrcamentoExternoRequest(
                    tokenAprovacao, DecisaoAprovacaoExterna.APROVAR))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void aprovacaoExterna_deveRejeitarTokenInvalido() throws Exception {
        var token = obterToken();
        var clienteId = criarCliente(token, "44.555.666/0001-81");
        var veiculoId = criarVeiculo(token, clienteId, "EXT0002");
        var pecaId = criarPeca(token, 10);
        var osId = criarOS(token, clienteId, veiculoId);

        avancarParaDiagnostico(token, osId, pecaId);
        mvc.perform(post("/api/ordens/{id}/gerar-orcamento", osId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        mvc.perform(post("/api/ordens/{id}/aprovar-externo", osId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AprovarOrcamentoExternoRequest(
                    "token-invalido", DecisaoAprovacaoExterna.APROVAR))))
            .andExpect(status().isUnauthorized());
    }

    // ── Helpers ────────────────────────────────────────────

    private void avancarParaDiagnostico(String token, String osId, String pecaId) throws Exception {
        mvc.perform(post("/api/ordens/{id}/iniciar-diagnostico", osId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/ordens/{id}/pecas", osId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AdicionarItemRequest(
                    java.util.UUID.fromString(pecaId), 1))))
            .andExpect(status().isOk());
    }

    private String obterToken() throws Exception {
        var resp = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resp).get("accessToken").asText();
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

    private String criarPeca(String token, int estoque) throws Exception {
        var resp = mvc.perform(post("/api/pecas")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarPecaRequest(
                    "Pastilha de freio", "", new BigDecimal("85.00"), estoque, 2))))
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
