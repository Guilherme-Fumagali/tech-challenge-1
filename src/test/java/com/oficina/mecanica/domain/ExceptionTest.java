package com.oficina.mecanica.domain;

import com.oficina.mecanica.domain.exception.EstoqueInsuficienteException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.exception.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ExceptionTest {

    @Test
    void recursoNaoEncontradoPorId() {
        var id = UUID.randomUUID();
        var ex = new RecursoNaoEncontradoException("Cliente", id);
        assertThat(ex.getMessage()).contains("Cliente").contains(id.toString());
    }

    @Test
    void recursoNaoEncontradoPorIdentificador() {
        var ex = new RecursoNaoEncontradoException("Veículo", "ABC1234");
        assertThat(ex.getMessage()).contains("Veículo").contains("ABC1234");
    }

    @Test
    void estoqueInsuficiente() {
        var ex = new EstoqueInsuficienteException("Filtro", 5, 2);
        assertThat(ex.getMessage())
            .contains("Filtro")
            .contains("solicitado=5")
            .contains("disponível=2");
    }

    @Test
    void transicaoInvalida() {
        var ex = new TransicaoInvalidaException("RECEBIDA → ENTREGUE");
        assertThat(ex.getMessage()).contains("RECEBIDA → ENTREGUE");
    }
}
