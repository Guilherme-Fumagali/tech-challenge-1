package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.servico.ServicoUseCase;
import com.oficina.mecanica.domain.entity.Servico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ServicoRepository;
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
class ServicoUseCaseTest {

    @Mock ServicoRepository repository;

    ServicoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ServicoUseCase(repository);
    }

    private Servico servico(UUID id) {
        return new Servico(id, "Troca de óleo", "Óleo sintético 5W30", new BigDecimal("120.00"), new BigDecimal("1.0"));
    }

    @Test
    void cadastrar_deveSalvarServico() {
        var servico = servico(UUID.randomUUID());
        when(repository.salvar(any())).thenReturn(servico);

        var resultado = useCase.cadastrar("Troca de óleo", "Óleo sintético 5W30", new BigDecimal("120.00"), new BigDecimal("1.0"));

        assertThat(resultado.getNome()).isEqualTo("Troca de óleo");
        verify(repository).salvar(any());
    }

    @Test
    void buscarPorId_deveRetornarServicoQuandoEncontrado() {
        var id = UUID.randomUUID();
        var servico = servico(id);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(servico));

        assertThat(useCase.buscarPorId(id)).isEqualTo(servico);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        var id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.buscarPorId(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarTodos_deveRetornarLista() {
        when(repository.listarTodos()).thenReturn(List.of(servico(UUID.randomUUID())));

        assertThat(useCase.listarTodos()).hasSize(1);
    }

    @Test
    void atualizar_deveAtualizarERetornarServico() {
        var id = UUID.randomUUID();
        var servico = servico(id);
        when(repository.buscarPorId(id)).thenReturn(Optional.of(servico));
        when(repository.salvar(servico)).thenReturn(servico);

        var resultado = useCase.atualizar(id, "Revisão completa", "Revisão geral", new BigDecimal("350.00"), new BigDecimal("3.0"));

        assertThat(resultado.getNome()).isEqualTo("Revisão completa");
        assertThat(resultado.getPrecoUnitario()).isEqualByComparingTo("350.00");
    }

    @Test
    void deletar_deveDeletarQuandoServicoExiste() {
        var id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.of(servico(id)));

        useCase.deletar(id);

        verify(repository).deletar(id);
    }

    @Test
    void deletar_deveLancarExcecaoQuandoNaoEncontrado() {
        var id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deletar(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).deletar(any());
    }
}
