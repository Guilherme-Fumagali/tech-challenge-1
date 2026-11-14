package com.oficina.mecanica.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.DocumentosDeTeste;
import com.oficina.mecanica.infrastructure.web.dto.request.CriarClienteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IdentidadeInternaIntegrationTest extends IntegracaoBase {

    private static final String CHAVE = "chave-interna-de-teste";
    private static final String CPF_FUNCIONARIO_SEMEADO = "39053344705";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void clienteCadastradoEhEncontradoComAChaveInterna() throws Exception {
        var cpf = DocumentosDeTeste.cpfValido();
        var resposta = mvc.perform(post("/api/clientes")
                .header("Authorization", "Bearer " + tokenFuncionario())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new CriarClienteRequest(cpf, "Carla", "carla@example.com", null))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        var clienteId = mapper.readTree(resposta).get("id").asText();

        mvc.perform(get("/internal/identidades/clientes/{cpf}", cpf).header("X-Chave-Interna", CHAVE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(clienteId))
            .andExpect(jsonPath("$.nome").value("Carla"))
            .andExpect(jsonPath("$.email").value("carla@example.com"))
            .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void funcionarioSemeadoTemOEmailDaMigrationV15() throws Exception {
        assertThat(jdbc.queryForObject("SELECT email FROM funcionarios WHERE cpf = ?", String.class,
            CPF_FUNCIONARIO_SEMEADO)).isEqualTo("funcionaria.teste@example.com");

        mvc.perform(get("/internal/identidades/funcionarios/{cpf}", "390.533.447-05")
                .header("X-Chave-Interna", CHAVE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Funcionária de Teste"))
            .andExpect(jsonPath("$.email").value("funcionaria.teste@example.com"))
            .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    void semChaveOuComChaveErradaRetorna401() throws Exception {
        mvc.perform(get("/internal/identidades/funcionarios/{cpf}", CPF_FUNCIONARIO_SEMEADO))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/internal/identidades/funcionarios/{cpf}", CPF_FUNCIONARIO_SEMEADO)
                .header("X-Chave-Interna", "outra"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/identidades/funcionarios/{cpf}", CPF_FUNCIONARIO_SEMEADO)
                .header("Authorization", "Bearer " + tokenFuncionario()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void documentoDesconhecidoRetorna404SemEcoarODocumento() throws Exception {
        var cpf = DocumentosDeTeste.cpfValido();

        mvc.perform(get("/internal/identidades/clientes/{cpf}", cpf).header("X-Chave-Interna", CHAVE))
            .andExpect(status().isNotFound())
            .andExpect(content().string(not(containsString(cpf))));
        mvc.perform(get("/internal/identidades/funcionarios/{cpf}", cpf).header("X-Chave-Interna", CHAVE))
            .andExpect(status().isNotFound());
    }

    @Test
    void documentoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/internal/identidades/clientes/{cpf}", "12345678900").header("X-Chave-Interna", CHAVE))
            .andExpect(status().isBadRequest());
    }
}
