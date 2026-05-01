package com.oficina.mecanica.domain;

import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.exception.EstoqueInsuficienteException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PecaTest {

    private Peca peca(int estoque, int minimo) {
        return new Peca(UUID.randomUUID(), "Filtro", "Filtro de ar",
            new BigDecimal("29.90"), estoque, minimo);
    }

    @Test
    void deveDecrementarEstoqueComSucesso() {
        var p = peca(10, 2);
        p.decrementarEstoque(3);
        assertThat(p.getQuantidadeEstoque()).isEqualTo(7);
    }

    @Test
    void deveLancarExcecaoSeEstoqueInsuficiente() {
        var p = peca(2, 0);
        assertThatThrownBy(() -> p.decrementarEstoque(5))
            .isInstanceOf(EstoqueInsuficienteException.class)
            .hasMessageContaining("solicitado=5")
            .hasMessageContaining("disponível=2");
    }

    @Test
    void deveIncrementarEstoque() {
        var p = peca(5, 3);
        p.incrementarEstoque(10);
        assertThat(p.getQuantidadeEstoque()).isEqualTo(15);
    }

    @Test
    void deveDetectarEstoqueAbaixoDoMinimo() {
        var p = peca(1, 5);
        assertThat(p.estaBaixoDoMinimo()).isTrue();
    }

    @Test
    void deveRetornarFalseQuandoEstoqueNaoEstaAbaixoDoMinimo() {
        var p = peca(10, 5);
        assertThat(p.estaBaixoDoMinimo()).isFalse();
    }
}
