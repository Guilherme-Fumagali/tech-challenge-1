package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.veiculo.VeiculoUseCase;
import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.entity.Veiculo;
import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.VeiculoRepository;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.domain.valueobject.Placa;
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
class VeiculoUseCaseTest {

    @Mock VeiculoRepository veiculoRepository;
    @Mock ClienteRepository clienteRepository;

    VeiculoUseCase useCase;

    private static final Placa PLACA = new Placa("ABC-1234");
    private static final UUID CLIENTE_ID = UUID.randomUUID();

    private Cliente cliente() {
        return new Cliente(CLIENTE_ID, new CpfCnpj("529.982.247-25"), "João", "j@e.com", "11999");
    }

    private Veiculo veiculo(UUID id) {
        return new Veiculo(id, PLACA, "Toyota", "Corolla", 2020, CLIENTE_ID);
    }

    @BeforeEach
    void setUp() {
        useCase = new VeiculoUseCase(veiculoRepository, clienteRepository);
    }

    @Test
    void cadastrar_deveSalvarQuandoClienteExisteEPlacaNaoExiste() {
        var id = UUID.randomUUID();
        when(clienteRepository.buscarPorId(CLIENTE_ID)).thenReturn(Optional.of(cliente()));
        when(veiculoRepository.existePorPlaca(PLACA)).thenReturn(false);
        when(veiculoRepository.salvar(any())).thenReturn(veiculo(id));

        var resultado = useCase.cadastrar(PLACA, "Toyota", "Corolla", 2020, CLIENTE_ID);

        assertThat(resultado.getMarca()).isEqualTo("Toyota");
        verify(veiculoRepository).salvar(any());
    }

    @Test
    void cadastrar_deveLancarExcecaoQuandoClienteNaoEncontrado() {
        when(clienteRepository.buscarPorId(CLIENTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.cadastrar(PLACA, "Toyota", "Corolla", 2020, CLIENTE_ID))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(veiculoRepository, never()).salvar(any());
    }

    @Test
    void cadastrar_deveLancarExcecaoQuandoPlacaJaExiste() {
        when(clienteRepository.buscarPorId(CLIENTE_ID)).thenReturn(Optional.of(cliente()));
        when(veiculoRepository.existePorPlaca(PLACA)).thenReturn(true);

        assertThatThrownBy(() -> useCase.cadastrar(PLACA, "Toyota", "Corolla", 2020, CLIENTE_ID))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("ABC1234");
        verify(veiculoRepository, never()).salvar(any());
    }

    @Test
    void buscarPorId_deveRetornarVeiculoQuandoEncontrado() {
        var id = UUID.randomUUID();
        when(veiculoRepository.buscarPorId(id)).thenReturn(Optional.of(veiculo(id)));

        assertThat(useCase.buscarPorId(id).getId()).isEqualTo(id);
    }

    @Test
    void buscarPorIdDoCliente_deveRetornarVeiculoDoProprioCliente() {
        var id = UUID.randomUUID();
        when(veiculoRepository.buscarPorId(id)).thenReturn(Optional.of(veiculo(id)));

        assertThat(useCase.buscarPorIdDoCliente(id, CLIENTE_ID).getId()).isEqualTo(id);
    }

    @Test
    void buscarPorIdDoCliente_deveTratarVeiculoDeOutroClienteComoInexistente() {
        var id = UUID.randomUUID();
        when(veiculoRepository.buscarPorId(id)).thenReturn(Optional.of(veiculo(id)));

        assertThatThrownBy(() -> useCase.buscarPorIdDoCliente(id, UUID.randomUUID()))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarPorId_deveLancarExcecaoQuandoNaoEncontrado() {
        var id = UUID.randomUUID();
        when(veiculoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.buscarPorId(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarTodos_deveRetornarLista() {
        when(veiculoRepository.listarTodos()).thenReturn(List.of(veiculo(UUID.randomUUID())));

        assertThat(useCase.listarTodos()).hasSize(1);
    }

    @Test
    void listarPorCliente_deveRetornarVeiculosDoCliente() {
        when(veiculoRepository.listarPorCliente(CLIENTE_ID)).thenReturn(List.of(veiculo(UUID.randomUUID())));

        assertThat(useCase.listarPorCliente(CLIENTE_ID)).hasSize(1);
    }

    @Test
    void atualizar_deveAtualizarERetornarVeiculo() {
        var id = UUID.randomUUID();
        var v = veiculo(id);
        when(veiculoRepository.buscarPorId(id)).thenReturn(Optional.of(v));
        when(veiculoRepository.salvar(v)).thenReturn(v);

        var resultado = useCase.atualizar(id, "Honda", "Civic", 2022);

        assertThat(resultado.getMarca()).isEqualTo("Honda");
        assertThat(resultado.getAnoFabricacao()).isEqualTo(2022);
    }

    @Test
    void deletar_deveDeletarQuandoVeiculoExiste() {
        var id = UUID.randomUUID();
        when(veiculoRepository.buscarPorId(id)).thenReturn(Optional.of(veiculo(id)));

        useCase.deletar(id);

        verify(veiculoRepository).deletar(id);
    }

    @Test
    void deletar_deveLancarExcecaoQuandoNaoEncontrado() {
        var id = UUID.randomUUID();
        when(veiculoRepository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deletar(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(veiculoRepository, never()).deletar(any());
    }
}
