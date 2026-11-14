package com.oficina.mecanica.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActuatorIntegrationTest extends IntegracaoBase {

    @Autowired MockMvc mvc;

    @Value("${spring.application.name}")
    String nomeDaAplicacao;

    @Test
    void sondasDeSaudeRespondemSemAutenticacao() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test
    void infoExpoeOCommitEmExecucao() throws Exception {
        mvc.perform(get("/actuator/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.git.commit.id").isNotEmpty())
            .andExpect(jsonPath("$.git.branch").isNotEmpty());
        assertThat(nomeDaAplicacao).isEqualTo("os-service");
    }
}
