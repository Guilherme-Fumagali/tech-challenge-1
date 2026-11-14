package com.oficina.mecanica.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.DocumentosDeTeste;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarClienteRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarOrdemServicoRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarVeiculoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SagaOrdemServicoIntegrationTest extends IntegracaoBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void aberturaDaOrdemIniciaASagaEPedeODiagnosticoAoExecution() throws Exception {
        var funcionario = tokenFuncionario();
        var clienteId = criarCliente(funcionario);
        var veiculoId = criarVeiculo(funcionario, clienteId);

        var resposta = mvc.perform(post("/api/ordens")
                .header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarOrdemServicoRequest(clienteId, veiculoId))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RECEBIDA"))
            .andExpect(jsonPath("$.sagaId").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        var osId = UUID.fromString(mapper.readTree(resposta).get("id").asText());
        var sagaId = mapper.readTree(resposta).get("sagaId").asText();

        var comando = FilasDeTeste.aguardarComando(FilasDeTeste.EXECUTION_COMANDOS, osId, "IniciarDiagnostico");
        assertThat(comando.envelope().path("sagaId").asText()).isEqualTo(sagaId);
        assertThat(comando.envelope().path("dados").path("clienteId").asText()).isEqualTo(clienteId.toString());
        assertThat(comando.envelope().path("dados").path("veiculo").path("modelo").asText()).isEqualTo("Onix");
        assertThat(comando.envelope().path("dados").path("abertaEm").asText()).endsWith("Z");

        mvc.perform(get("/api/ordens/{id}/saga", osId).header("Authorization", "Bearer " + funcionario))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sagaId))
            .andExpect(jsonPath("$.estado").value("AGUARDANDO_FILA_DIAGNOSTICO"))
            .andExpect(jsonPath("$.ultimoComando").value("IniciarDiagnostico"))
            .andExpect(jsonPath("$.passos", hasSize(1)))
            .andExpect(jsonPath("$.passos[0].direcao").value("ENVIADO"))
            .andExpect(jsonPath("$.passos[0].mensagemId").value(comando.mensagemId().toString()));

        mvc.perform(get("/api/ordens/{id}/historico", osId).header("Authorization", "Bearer " + funcionario))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].statusDestino").value("RECEBIDA"))
            .andExpect(jsonPath("$[0].evento").value("OrdemServicoAberta"));

        mvc.perform(get("/api/ordens/{id}/historico", osId).header("Authorization", "Bearer " + tokenCliente(clienteId)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/ordens/{id}/historico", osId)
                .header("Authorization", "Bearer " + tokenCliente(UUID.randomUUID())))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/ordens/{id}/saga", osId).header("Authorization", "Bearer " + tokenCliente(clienteId)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/ordens/{id}/saga", UUID.randomUUID()).header("Authorization", "Bearer " + funcionario))
            .andExpect(status().isNotFound());
    }

    @Test
    void prazoVencidoReenviaOComandoComNovaIdentificacaoEDepoisLevaAFalha() throws Exception {
        var funcionario = tokenFuncionario();
        var clienteId = criarCliente(funcionario);
        var osId = abrirOrdem(funcionario, clienteId, criarVeiculo(funcionario, clienteId));
        var original = FilasDeTeste.aguardarComando(FilasDeTeste.EXECUTION_COMANDOS, osId, "IniciarDiagnostico");

        jdbc.update("UPDATE sagas_ordem_servico SET prazo_resposta = prazo_resposta - INTERVAL '1 hour' WHERE os_id = ?",
            osId);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(
            FilasDeTeste.comandosRecebidos(FilasDeTeste.EXECUTION_COMANDOS, osId, "IniciarDiagnostico")).hasSize(2));
        var reenvio = FilasDeTeste.comandosRecebidos(FilasDeTeste.EXECUTION_COMANDOS, osId, "IniciarDiagnostico").get(1);
        assertThat(reenvio.mensagemId()).isNotEqualTo(original.mensagemId());
        assertThat(reenvio.envelope().path("dados")).isEqualTo(original.envelope().path("dados"));
        assertThat(jdbc.queryForObject("SELECT tentativas_reenvio FROM sagas_ordem_servico WHERE os_id = ?",
            Integer.class, osId)).isEqualTo(1);

        jdbc.update("UPDATE sagas_ordem_servico SET tentativas_reenvio = 3, "
            + "prazo_resposta = prazo_resposta - INTERVAL '1 hour' WHERE os_id = ?", osId);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(jdbc.queryForObject(
            "SELECT estado FROM sagas_ordem_servico WHERE os_id = ?", String.class, osId)).isEqualTo("FALHA"));
        assertThat(jdbc.queryForObject("SELECT prazo_resposta FROM sagas_ordem_servico WHERE os_id = ?",
            java.sql.Timestamp.class, osId)).isNull();
        mvc.perform(get("/api/ordens/{id}/saga", osId).header("Authorization", "Bearer " + funcionario))
            .andExpect(jsonPath("$.passos[-1:].tipo").value("PrazoEsgotado"));
    }

    private UUID criarCliente(String token) throws Exception {
        var resposta = mvc.perform(post("/api/clientes")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarClienteRequest(DocumentosDeTeste.cpfValido(),
                    "Cliente da Saga", "cliente.saga@example.com", null))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resposta).get("id").asText());
    }

    private UUID criarVeiculo(String token, UUID clienteId) throws Exception {
        var resposta = mvc.perform(post("/api/veiculos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarVeiculoRequest(DocumentosDeTeste.placaValida(),
                    "Chevrolet", "Onix", 2021, clienteId))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resposta).get("id").asText());
    }

    private UUID abrirOrdem(String token, UUID clienteId, UUID veiculoId) throws Exception {
        var resposta = mvc.perform(post("/api/ordens")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarOrdemServicoRequest(clienteId, veiculoId))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return UUID.fromString(mapper.readTree(resposta).get("id").asText());
    }
}
