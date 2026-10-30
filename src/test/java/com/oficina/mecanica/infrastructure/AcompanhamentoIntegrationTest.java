package com.oficina.mecanica.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.DocumentosDeTeste;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarClienteRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarOrdemServicoRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarVeiculoRequest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AcompanhamentoIntegrationTest extends IntegracaoBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CircuitBreakerRegistry circuitBreakers;
    @Autowired MeterRegistry registry;

    String funcionario;
    UUID clienteId;
    UUID osId;

    @BeforeEach
    void abrirOrdem() throws Exception {
        PARTICIPANTES.resetAll();
        funcionario = tokenFuncionario();
        clienteId = criarCliente();
        osId = abrirOrdem(clienteId, criarVeiculo(clienteId));
    }

    @AfterEach
    void fecharCircuitos() {
        circuitBreakers.circuitBreaker("billing").reset();
        circuitBreakers.circuitBreaker("execution").reset();
    }

    @Test
    void compoeOrdemHistoricoOrcamentoEExecucaoRepassandoOToken() throws Exception {
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/orcamentos")).withQueryParam("osId", equalTo(osId.toString()))
            .willReturn(okJson("""
                [{"id": "b6f1", "status": "APROVADO", "total": 210.00,
                  "pagamento": {"status": "PENDENTE", "linkPagamento": "https://pagamento.example/abc"}}]
                """)));
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/execucoes/" + osId))
            .willReturn(okJson("{\"osId\": \"%s\", \"status\": \"EM_REPARO\", \"etapaAtual\": \"REPARO\"}"
                .formatted(osId))));

        mvc.perform(acompanhamento(funcionario))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordemServico.id").value(osId.toString()))
            .andExpect(jsonPath("$.historico", hasSize(1)))
            .andExpect(jsonPath("$.orcamento.status").value("APROVADO"))
            .andExpect(jsonPath("$.orcamento.pagamento.status").value("PENDENTE"))
            .andExpect(jsonPath("$.execucao.status").value("EM_REPARO"))
            .andExpect(jsonPath("$.fontesIndisponiveis", empty()));

        PARTICIPANTES.verify(getRequestedFor(urlPathEqualTo("/api/orcamentos"))
            .withHeader("Authorization", equalTo("Bearer " + funcionario)));
        PARTICIPANTES.verify(getRequestedFor(urlPathEqualTo("/api/execucoes/" + osId))
            .withHeader("Authorization", equalTo("Bearer " + funcionario)));
    }

    @Test
    void participanteLentoAlemDoLimiteViraFonteIndisponivel() throws Exception {
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/orcamentos")).willReturn(okJson("{\"status\": \"GERADO\"}")));
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/execucoes/" + osId))
            .willReturn(okJson("{\"status\": \"EM_DIAGNOSTICO\"}").withFixedDelay(3500)));

        var inicio = System.nanoTime();
        mvc.perform(acompanhamento(funcionario))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcamento.status").value("GERADO"))
            .andExpect(jsonPath("$.execucao").doesNotExist())
            .andExpect(jsonPath("$.fontesIndisponiveis", contains("execution")));
        var decorridoMs = (System.nanoTime() - inicio) / 1_000_000;

        assertThat(decorridoMs).isLessThan(3_300);
        assertThat(registry.counter("oficina.integracao.falhas", "integracao", "execution",
            "motivo", "TimeoutException").count()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void circuitoAbertoRespondeSemChamarOParticipante() throws Exception {
        circuitBreakers.circuitBreaker("billing").transitionToOpenState();
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/execucoes/" + osId))
            .willReturn(okJson("{\"status\": \"AGUARDANDO_DIAGNOSTICO\"}")));

        mvc.perform(acompanhamento(funcionario))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcamento").doesNotExist())
            .andExpect(jsonPath("$.execucao.status").value("AGUARDANDO_DIAGNOSTICO"))
            .andExpect(jsonPath("$.fontesIndisponiveis", contains("billing")));

        PARTICIPANTES.verify(0, getRequestedFor(urlPathEqualTo("/api/orcamentos")));
    }

    @Test
    void falhasSeguidasAbremOCircuito() throws Exception {
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/orcamentos")).willReturn(serverError()));
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/execucoes/" + osId)).willReturn(notFound()));

        for (int i = 0; i < 10; i++) {
            mvc.perform(acompanhamento(funcionario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontesIndisponiveis", contains("billing")));
        }

        assertThat(circuitBreakers.circuitBreaker("billing").getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreakers.circuitBreaker("execution").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void ausenciaDeDadosNoParticipanteNaoEhIndisponibilidade() throws Exception {
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/orcamentos")).willReturn(okJson("[]")));
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/execucoes/" + osId)).willReturn(notFound()));

        mvc.perform(acompanhamento(funcionario))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcamento").doesNotExist())
            .andExpect(jsonPath("$.execucao").doesNotExist())
            .andExpect(jsonPath("$.fontesIndisponiveis", empty()));
    }

    @Test
    void erroDeAutorizacaoNoParticipanteNaoAbreOCircuito() throws Exception {
        PARTICIPANTES.stubFor(get(urlPathEqualTo("/api/orcamentos")).willReturn(forbidden()));

        for (int i = 0; i < 10; i++) {
            mvc.perform(acompanhamento(funcionario))
                .andExpect(jsonPath("$.fontesIndisponiveis", contains("billing")));
        }

        assertThat(circuitBreakers.circuitBreaker("billing").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void clienteAcompanhaApenasAPropriaOrdem() throws Exception {
        mvc.perform(acompanhamento(tokenCliente(clienteId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ordemServico.id").value(osId.toString()));
        mvc.perform(acompanhamento(tokenCliente(UUID.randomUUID())))
            .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.RequestBuilder acompanhamento(String token) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get("/api/ordens/{id}/acompanhamento", osId)
            .header("Authorization", "Bearer " + token);
    }

    private UUID criarCliente() throws Exception {
        var resposta = mvc.perform(post("/api/clientes")
                .header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarClienteRequest(DocumentosDeTeste.cpfValido(),
                    "Cliente Acompanhamento", null, null))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resposta).get("id").asText());
    }

    private UUID criarVeiculo(UUID cliente) throws Exception {
        var resposta = mvc.perform(post("/api/veiculos")
                .header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarVeiculoRequest(DocumentosDeTeste.placaValida(),
                    "Renault", "Kwid", 2022, cliente))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resposta).get("id").asText());
    }

    private UUID abrirOrdem(UUID cliente, UUID veiculo) throws Exception {
        var resposta = mvc.perform(post("/api/ordens")
                .header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarOrdemServicoRequest(cliente, veiculo))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resposta).get("id").asText());
    }
}
