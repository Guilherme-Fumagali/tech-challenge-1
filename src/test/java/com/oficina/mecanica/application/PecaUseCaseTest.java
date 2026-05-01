package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.peca.PecaUseCase;
import com.oficina.mecanica.domain.entity.Peca;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.PecaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecaUseCaseTest {

    @Mock PecaRepository repository;

    PecaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PecaUseCase(repository);
    }

    private Peca peca(UUID id) {
        return new Peca(id, "Filtro de óleo", "Mann W712", new BigDecimal("45.00"), 10, 3);
    }

    @Test
    void cadastrar_deveSalvarPeca() {
        when(repository.salvar(any())).thenReturn(peca(UUID.randomUUID()));

        var resultado = useCase.cadastrar("Filtro de óleo", "Mann W712", new BigDecimal("45.00"), 10, 3);

        assertThat(resultado.getNome()).isEqualTo("Filtro de óleo");
        verify(repository).salvar(any());
    }

    @Test
    void buscarPorId_deveRetornarPecaQuandoEncontrada() {
        var id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(peca(id)));

        assertThat(useCase.buscarPorId(id).getId()).isEqualTo(id);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrada() {
        var id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.buscarPorId(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarTodas_deveRetornarLista() {
        when(repository.listarTodos()).thenReturn(List.of(peca(UUID.randomUUID())));

        assertThat(useCase.listarTodas()).hasSize(1);
    }

    @Test
    void listarAbaixoDoEstoqueMinimo_deveRetornarLista() {
        when(repository.listarAbaixoDoEstoqueMinimo()).thenReturn(List.of(peca(UUID.randomUUID())));

        assertThat(useCase.listarAbaixoDoEstoqueMinimo()).hasSize(1);
    }

    @Test
    void atualizar_deveAtualizarERetornarPeca() {
        var id = UUID.randomUUID();
        var peca = peca(id);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(peca));
        when(repository.salvar(peca)).thenReturn(peca);

        var resultado = useCase.atualizar(id, "Filtro ar", "Bosch", new BigDecimal("35.00"), 2);

        assertThat(resultado.getNome()).isEqualTo("Filtro ar");
        assertThat(resultado.getPrecoUnitario()).isEqualByComparingTo("35.00");
    }

    @Test
    void incrementarEstoque_deveIncrementarQuantidade() {
        var id = UUID.randomUUID();
        var peca = peca(id); // estoque = 10
        when(repository.buscarPorId(id)).thenReturn(Optional.of(peca));
        when(repository.salvar(peca)).thenReturn(peca);

        useCase.incrementarEstoque(id, 5);

        assertThat(peca.getQuantidadeEstoque()).isEqualTo(15);
        verify(repository).salvar(peca);
    }

    @Test
    void deletar_deveDeletarQuandoPecaExiste() {
        var id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(peca(id)));

        useCase.deletar(id);

        verify(repository).deletar(id);
    }

    @Test
    void deletar_deveLancarExcecaoQuandoNaoEncontrada() {
        var id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deletar(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).deletar(any());
    }
}
