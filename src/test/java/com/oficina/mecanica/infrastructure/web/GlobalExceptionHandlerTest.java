package com.oficina.mecanica.infrastructure.web;

import com.oficina.mecanica.application.port.MetricasOrdemServico;
import com.oficina.mecanica.infrastructure.persistence.entity.PecaJpaEntity;
import com.oficina.mecanica.infrastructure.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(mock(MetricasOrdemServico.class));

    @Test
    void conflitoDeVersaoRetorna409() {
        var resposta = handler.handleConcorrencia(
            new ObjectOptimisticLockingFailureException(PecaJpaEntity.class, UUID.randomUUID()));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().getDetail()).contains("alterado por outra operação");
    }

    @Test
    void erroInesperadoRetorna500SemDetalhesInternos() {
        var resposta = handler.handleGenerico(new IllegalStateException("senha=123 na conexão jdbc"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().getDetail()).isEqualTo("Erro interno. Tente novamente mais tarde.");
    }

    @Test
    void excecaoComStatusDoProtocoloMantemOStatus() {
        var resposta = handler.handleGenerico(new HttpRequestMethodNotSupportedException("PATCH"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void violacaoDeIntegridadeRetorna409() {
        var resposta = handler.handleIntegridade(new DataIntegrityViolationException("fk_veiculos_cliente"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
