package com.oficina.mecanica.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.DocumentosDeTeste;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarClienteRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarOrdemServicoRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarVeiculoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SegurancaEErrosIntegrationTest extends IntegracaoBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void semTokenOuComTokenInvalidoRespondeUmUnico401EmJson() throws Exception {
        mvc.perform(get("/api/ordens"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Autenticação obrigatória."));
        mvc.perform(post("/api/pecas").header("Authorization", "Bearer token-invalido")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Autenticação obrigatória."));
    }

    @Test
    void papelSemPermissaoRecebe403EmJson() throws Exception {
        mvc.perform(get("/api/clientes").header("Authorization", "Bearer " + tokenCliente(UUID.randomUUID())))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void corpoMalformadoRetorna400() throws Exception {
        mvc.perform(post("/api/pecas").header("Authorization", "Bearer " + tokenFuncionario())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Corpo da requisição ausente ou em formato inválido."));
    }

    @Test
    void errosDoProtocoloMantemOStatusOriginal() throws Exception {
        mvc.perform(patch("/api/pecas/{id}", UUID.randomUUID()).header("Authorization", "Bearer " + tokenFuncionario()))
            .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/inexistente").header("Authorization", "Bearer " + tokenFuncionario()))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/pecas").header("Authorization", "Bearer " + tokenFuncionario())
                .contentType(MediaType.TEXT_PLAIN).content("x"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void exclusaoDeClienteComOrdemRetorna409() throws Exception {
        var funcionario = tokenFuncionario();
        var cliente = mapper.readTree(mvc.perform(post("/api/clientes").header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarClienteRequest(DocumentosDeTeste.cpfValido(), "Diego",
                    null, null))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        var veiculo = mapper.readTree(mvc.perform(post("/api/veiculos").header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarVeiculoRequest(DocumentosDeTeste.placaValida(), "Ford",
                    "Ka", 2018, UUID.fromString(cliente)))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/api/ordens").header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarOrdemServicoRequest(UUID.fromString(cliente),
                    UUID.fromString(veiculo)))))
            .andExpect(status().isCreated());

        mvc.perform(delete("/api/clientes/{id}", cliente).header("Authorization", "Bearer " + funcionario))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value(
                "A operação viola uma restrição de integridade, por exemplo um registro ainda referenciado."));
    }

    @Test
    void statusPublicoDevolveApenasIdStatusEData() throws Exception {
        var funcionario = tokenFuncionario();
        var cliente = mapper.readTree(mvc.perform(post("/api/clientes").header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarClienteRequest(DocumentosDeTeste.cpfValido(), "Eva",
                    null, null))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        var veiculo = mapper.readTree(mvc.perform(post("/api/veiculos").header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarVeiculoRequest(DocumentosDeTeste.placaValida(), "Fiat",
                    "Mobi", 2020, UUID.fromString(cliente)))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        var os = mapper.readTree(mvc.perform(post("/api/ordens").header("Authorization", "Bearer " + funcionario)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarOrdemServicoRequest(UUID.fromString(cliente),
                    UUID.fromString(veiculo)))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();

        mvc.perform(get("/api/ordens/{id}/status", os))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", aMapWithSize(3)))
            .andExpect(jsonPath("$.id").value(os))
            .andExpect(jsonPath("$.status").value("RECEBIDA"))
            .andExpect(jsonPath("$.atualizadoEm").isNotEmpty())
            .andExpect(jsonPath("$.clienteId").doesNotExist());
    }
}
