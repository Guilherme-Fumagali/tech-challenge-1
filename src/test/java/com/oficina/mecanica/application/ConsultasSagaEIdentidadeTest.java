package com.oficina.mecanica.application;

import com.oficina.mecanica.application.usecase.identidade.ConsultarIdentidadeUseCase;
import com.oficina.mecanica.application.usecase.ordemservico.ConsultarHistoricoOSUseCase;
import com.oficina.mecanica.application.usecase.saga.ConsultarSagaUseCase;
import com.oficina.mecanica.domain.entity.Cliente;
import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.Funcionario;
import com.oficina.mecanica.domain.entity.HistoricoStatus;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.IdentidadeNaoEncontradaException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.exception.SagaNaoEncontradaException;
import com.oficina.mecanica.domain.repository.ClienteRepository;
import com.oficina.mecanica.domain.repository.FuncionarioRepository;
import com.oficina.mecanica.domain.repository.HistoricoStatusRepository;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.domain.valueobject.StatusFuncionario;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultasSagaEIdentidadeTest {

    @Mock SagaOrdemServicoRepository sagaRepository;
    @Mock OrdemServicoRepository osRepository;
    @Mock HistoricoStatusRepository historicoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock FuncionarioRepository funcionarioRepository;

    @Test
    void consultaDaSagaTrazOsPassos() {
        var osId = UUID.randomUUID();
        var saga = SagaOrdemServico.iniciar(UUID.randomUUID(), osId, Instant.now());
        when(sagaRepository.buscarPorOsId(osId)).thenReturn(Optional.of(saga));
        when(sagaRepository.listarPassos(saga.getId())).thenReturn(List.of());

        var resumo = new ConsultarSagaUseCase(sagaRepository).executar(osId);

        assertThat(resumo.saga()).isSameAs(saga);
        assertThat(resumo.passos()).isEmpty();
    }

    @Test
    void sagaInexistenteResultaEm404() {
        var osId = UUID.randomUUID();
        when(sagaRepository.buscarPorOsId(osId)).thenReturn(Optional.empty());
        var useCase = new ConsultarSagaUseCase(sagaRepository);

        assertThatThrownBy(() -> useCase.executar(osId)).isInstanceOf(SagaNaoEncontradaException.class);
    }

    @Test
    void historicoDaOrdemParaFuncionario() {
        var osId = UUID.randomUUID();
        var registro = new HistoricoStatus(osId, null, StatusOS.RECEBIDA, "OrdemServicoAberta", LocalDateTime.now());
        when(osRepository.existePorId(osId)).thenReturn(true);
        when(historicoRepository.listarPorOrdemServico(osId)).thenReturn(List.of(registro));

        assertThat(new ConsultarHistoricoOSUseCase(osRepository, historicoRepository).executar(osId))
            .containsExactly(registro);
    }

    @Test
    void historicoDeOrdemInexistente() {
        var osId = UUID.randomUUID();
        when(osRepository.existePorId(osId)).thenReturn(false);
        var useCase = new ConsultarHistoricoOSUseCase(osRepository, historicoRepository);

        assertThatThrownBy(() -> useCase.executar(osId)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void clienteSoVeOHistoricoDaPropriaOrdem() {
        var clienteId = UUID.randomUUID();
        var os = OrdemServico.reconstituir(DadosOrdemServico.builder().id(UUID.randomUUID()).clienteId(clienteId)
            .veiculoId(UUID.randomUUID()).status(StatusOS.RECEBIDA).dataAbertura(LocalDateTime.now()).build());
        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(historicoRepository.listarPorOrdemServico(os.getId())).thenReturn(List.of());
        var useCase = new ConsultarHistoricoOSUseCase(osRepository, historicoRepository);

        assertThat(useCase.executarDoCliente(os.getId(), clienteId)).isEmpty();
        var outroCliente = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.executarDoCliente(os.getId(), outroCliente))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void identidadeDeClientePorCpf() {
        var cliente = new Cliente(UUID.randomUUID(), new CpfCnpj("529.982.247-25"), "Ana", "ana@example.com", null);
        when(clienteRepository.buscarPorCpfCnpj(new CpfCnpj("52998224725"))).thenReturn(Optional.of(cliente));

        var identidade = new ConsultarIdentidadeUseCase(clienteRepository, funcionarioRepository)
            .cliente("529.982.247-25");

        assertThat(identidade.id()).isEqualTo(cliente.getId());
        assertThat(identidade.status()).isEqualTo("ATIVO");
        assertThat(identidade.email()).isEqualTo("ana@example.com");
    }

    @Test
    void identidadeDeFuncionarioPorCpf() {
        var funcionario = new Funcionario(UUID.randomUUID(), "39053344705", "Bia", "bia@example.com",
            StatusFuncionario.INATIVO);
        when(funcionarioRepository.buscarPorCpf("39053344705")).thenReturn(Optional.of(funcionario));

        var identidade = new ConsultarIdentidadeUseCase(clienteRepository, funcionarioRepository)
            .funcionario("390.533.447-05");

        assertThat(identidade.nome()).isEqualTo("Bia");
        assertThat(identidade.status()).isEqualTo("INATIVO");
    }

    @Test
    void identidadeInexistenteNaoRevelaODocumento() {
        when(clienteRepository.buscarPorCpfCnpj(any())).thenReturn(Optional.empty());
        var useCase = new ConsultarIdentidadeUseCase(clienteRepository, funcionarioRepository);

        assertThatThrownBy(() -> useCase.cliente("52998224725"))
            .isInstanceOf(IdentidadeNaoEncontradaException.class)
            .hasMessage("Identidade não encontrada.");
    }

    @Test
    void funcionarioNaoEhIdentificadoPorCnpj() {
        var useCase = new ConsultarIdentidadeUseCase(clienteRepository, funcionarioRepository);

        assertThatThrownBy(() -> useCase.funcionario("11.222.333/0001-81")).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> useCase.funcionario("123")).isInstanceOf(DomainException.class);
        verify(funcionarioRepository, never()).buscarPorCpf(any());
    }

    @Test
    void funcionarioInexistente() {
        when(funcionarioRepository.buscarPorCpf("39053344705")).thenReturn(Optional.empty());
        var useCase = new ConsultarIdentidadeUseCase(clienteRepository, funcionarioRepository);

        assertThatThrownBy(() -> useCase.funcionario("39053344705"))
            .isInstanceOf(IdentidadeNaoEncontradaException.class);
    }
}
