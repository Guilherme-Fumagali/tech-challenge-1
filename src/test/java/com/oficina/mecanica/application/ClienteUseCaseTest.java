package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.cliente.ClienteUseCase;
import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.domain.valueobject.StatusCliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteUseCaseTest {

    @Mock ClienteRepository repository;

    ClienteUseCase useCase;

    private static final CpfCnpj CPF = new CpfCnpj("529.982.247-25");

    @BeforeEach
    void setUp() {
        useCase = new ClienteUseCase(repository);
    }

    @Test
    void clienteNovoNasceAtivo() {
        var cliente = new Cliente(UUID.randomUUID(), CPF, "João", "joao@email.com", "11999999999");

        assertThat(cliente.getStatus()).isEqualTo(StatusCliente.ATIVO);
    }

    @Test
    void alterarStatus_deveBloquearClienteExistente() {
        var id = UUID.randomUUID();
        var cliente = new Cliente(id, CPF, "João", "joao@email.com", "11999999999");
        when(repository.buscarPorId(id)).thenReturn(Optional.of(cliente));
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = useCase.alterarStatus(id, StatusCliente.BLOQUEADO);

        assertThat(resultado.getStatus()).isEqualTo(StatusCliente.BLOQUEADO);
        verify(repository).salvar(cliente);
    }

    @Test
    void cadastrar_deveSalvarQuandoCpfNaoExiste() {
        var cliente = new Cliente(UUID.randomUUID(), CPF, "João", "joao@email.com", "11999999999");
        when(repository.existePorCpfCnpj(CPF)).thenReturn(false);
        when(repository.salvar(any())).thenReturn(cliente);

        var resultado = useCase.cadastrar(CPF, "João", "joao@email.com", "11999999999");

        assertThat(resultado.getNome()).isEqualTo("João");
        verify(repository).salvar(any());
    }

    @Test
    void cadastrar_deveLancarExcecaoQuandoCpfJaExiste() {
        when(repository.existePorCpfCnpj(CPF)).thenReturn(true);

        assertThatThrownBy(() -> useCase.cadastrar(CPF, "João", "joao@email.com", "11999"))
            .isInstanceOf(DomainException.class);
        verify(repository, never()).salvar(any());
    }

    @Test
    void buscarPorId_deveRetornarClienteQuandoEncontrado() {
        var id = UUID.randomUUID();
        var cliente = new Cliente(id, CPF, "Maria", "maria@email.com", "11888888888");
        when(repository.buscarPorId(id)).thenReturn(Optional.of(cliente));

        assertThat(useCase.buscarPorId(id)).isEqualTo(cliente);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        var id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.buscarPorId(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarPorCpfCnpj_deveRetornarClienteQuandoEncontrado() {
        var cliente = new Cliente(UUID.randomUUID(), CPF, "Ana", "ana@email.com", "11777777777");
        when(repository.buscarPorCpfCnpj(any())).thenReturn(Optional.of(cliente));

        assertThat(useCase.buscarPorCpfCnpj("529.982.247-25")).isEqualTo(cliente);
    }

    @Test
    void buscarPorCpfCnpj_deveLancarExcecaoQuandoNaoEncontrado() {
        when(repository.buscarPorCpfCnpj(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.buscarPorCpfCnpj("529.982.247-25"))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarTodos_deveRetornarLista() {
        var clientes = List.of(new Cliente(UUID.randomUUID(), CPF, "João", "j@e.com", "11999"));
        when(repository.listarTodos()).thenReturn(clientes);

        assertThat(useCase.listarTodos()).hasSize(1);
    }

    @Test
    void atualizar_deveAtualizarERetornarCliente() {
        var id = UUID.randomUUID();
        var cliente = new Cliente(id, CPF, "João", "joao@email.com", "11999");
        when(repository.buscarPorId(id)).thenReturn(Optional.of(cliente));
        when(repository.salvar(cliente)).thenReturn(cliente);

        var resultado = useCase.atualizar(id, "João Silva", "novo@email.com", "11000");

        assertThat(resultado.getNome()).isEqualTo("João Silva");
        assertThat(resultado.getEmail()).isEqualTo("novo@email.com");
        verify(repository).salvar(cliente);
    }

    @Test
    void deletar_deveDeletarQuandoClienteExiste() {
        var id = UUID.randomUUID();
        var cliente = new Cliente(id, CPF, "João", "j@e.com", "11999");
        when(repository.buscarPorId(id)).thenReturn(Optional.of(cliente));

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
