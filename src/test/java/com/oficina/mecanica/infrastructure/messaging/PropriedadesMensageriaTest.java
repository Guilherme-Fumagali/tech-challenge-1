package com.oficina.mecanica.infrastructure.messaging;

import com.oficina.mecanica.domain.saga.Participante;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropriedadesMensageriaTest {

    private final PropriedadesMensageria propriedades = new PropriedadesMensageria("os-service",
        new PropriedadesMensageria.Filas("oficina-staging-os-eventos", "oficina-staging-billing-comandos",
            "oficina-staging-execution-comandos", "oficina-staging-os-eventos"),
        "oficina-staging-os-eventos-dlq", new PropriedadesMensageria.Relay(500, 50));

    @Test
    void chaveLogicaDaFilaPorParticipante() {
        assertThat(propriedades.chaveDaFila(Participante.BILLING)).isEqualTo("billing-comandos");
        assertThat(propriedades.chaveDaFila(Participante.EXECUTION)).isEqualTo("execution-comandos");
    }

    @Test
    void chaveLogicaResolvidaParaONomeDoAmbiente() {
        assertThat(propriedades.nomeDaFila("os-eventos")).isEqualTo("oficina-staging-os-eventos");
        assertThat(propriedades.nomeDaFila("billing-comandos")).isEqualTo("oficina-staging-billing-comandos");
        assertThat(propriedades.nomeDaFila("execution-comandos")).isEqualTo("oficina-staging-execution-comandos");
        assertThatThrownBy(() -> propriedades.nomeDaFila("outra")).isInstanceOf(IllegalArgumentException.class);
    }
}
