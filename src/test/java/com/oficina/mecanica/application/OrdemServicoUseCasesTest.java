package com.oficina.mecanica.application;

import com.oficina.mecanica.application.port.NotificacaoService;
import com.oficina.mecanica.application.usecase.ordemservico.*;
import com.oficina.mecanica.domain.entity.*;
import com.oficina.mecanica.domain.exception.DomainException;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.*;
import com.oficina.mecanica.domain.valueobject.CpfCnpj;
import com.oficina.mecanica.domain.valueobject.Placa;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoUseCasesTest {

    @Mock OrdemServicoRepository osRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock VeiculoRepository veiculoRepository;
    @Mock ServicoRepository servicoRepository;
    @Mock NotificacaoService notificacaoService;

    private OrdemServico osComStatus(StatusOS status) {
        return OrdemServico.reconstituir()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(status).dataAbertura(LocalDateTime.now()).build();
    }

    private OrdemServico osEmExecucao() {
        return OrdemServico.reconstituir()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(StatusOS.EM_EXECUCAO).dataAbertura(LocalDateTime.now()).dataInicio(LocalDateTime.now())
            .build();
    }

    private OrdemServico osFinalizada() {
        return OrdemServico.reconstituir()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(StatusOS.FINALIZADA).dataAbertura(LocalDateTime.now())
            .dataInicio(LocalDateTime.now()).dataConclusao(LocalDateTime.now()).build();
    }

    // ── IniciarDiagnosticoUseCase ─────────────────────────────────────────

    @Test
    void iniciarDiagnostico_deveMudarStatusParaEmDiagnostico() {
        var os = osComStatus(StatusOS.RECEBIDA);
        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(osRepository.salvar(os)).thenReturn(os);

        new IniciarDiagnosticoUseCase(osRepository).executar(os.getId());

        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        verify(osRepository).salvar(os);
    }

    @Test
    void iniciarDiagnostico_deveLancarExcecaoSeNaoEncontrada() {
        var id = UUID.randomUUID();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());

        var uc = new IniciarDiagnosticoUseCase(osRepository);
        assertThatThrownBy(() -> uc.executar(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ── AprovarOrcamentoUseCase ───────────────────────────────────────────

    @Test
    void aprovar_deveMudarStatusParaEmExecucao() {
        var os = osComStatus(StatusOS.AGUARDANDO_APROVACAO);
        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(osRepository.salvar(os)).thenReturn(os);

        new AprovarOrcamentoUseCase(osRepository).executar(os.getId());

        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void aprovar_deveLancarExcecaoSeNaoEncontrada() {
        var id = UUID.randomUUID();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());

        var uc = new AprovarOrcamentoUseCase(osRepository);
        assertThatThrownBy(() -> uc.executar(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ── ConcluirServicosUseCase ───────────────────────────────────────────

    @Test
    void concluir_deveMudarStatusParaFinalizada() {
        var os = osEmExecucao();
        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(osRepository.salvar(os)).thenReturn(os);

        new ConcluirServicosUseCase(osRepository).executar(os.getId());

        assertThat(os.getStatus()).isEqualTo(StatusOS.FINALIZADA);
        assertThat(os.getDataConclusao()).isNotNull();
    }

    // ── EntregarVeiculoUseCase ────────────────────────────────────────────

    @Test
    void entregar_deveMudarStatusParaEntregue() {
        var os = osFinalizada();
        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(osRepository.salvar(os)).thenReturn(os);

        new EntregarVeiculoUseCase(osRepository).executar(os.getId());

        assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
    }

    // ── ConsultarStatusOSUseCase ──────────────────────────────────────────

    @Test
    void consultarStatus_deveRetornarOS() {
        var os = osComStatus(StatusOS.RECEBIDA);
        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));

        var resultado = new ConsultarStatusOSUseCase(osRepository).executar(os.getId());

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.RECEBIDA);
    }

    @Test
    void consultarStatus_deveLancarExcecaoSeNaoEncontrada() {
        var id = UUID.randomUUID();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());

        var uc = new ConsultarStatusOSUseCase(osRepository);
        assertThatThrownBy(() -> uc.executar(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void consultarStatus_listarTodas_deveRetornarLista() {
        when(osRepository.listarTodas()).thenReturn(List.of(osComStatus(StatusOS.RECEBIDA)));

        assertThat(new ConsultarStatusOSUseCase(osRepository).listarTodas()).hasSize(1);
    }

    // ── CriarOrdemServicoUseCase ──────────────────────────────────────────

    @Test
    void criarOS_deveCriarQuandoClienteEVeiculoSaoValidos() {
        var clienteId = UUID.randomUUID();
        var veiculoId = UUID.randomUUID();
        var cliente = new Cliente(clienteId, new CpfCnpj("529.982.247-25"), "João", "j@e.com", "11999");
        var veiculo = new Veiculo(veiculoId, new Placa("ABC-1234"), "Toyota", "Corolla", 2020, clienteId);
        var os = osComStatus(StatusOS.RECEBIDA);

        when(clienteRepository.buscarPorId(clienteId)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.buscarPorId(veiculoId)).thenReturn(Optional.of(veiculo));
        when(osRepository.salvar(any())).thenReturn(os);

        var resultado = new CriarOrdemServicoUseCase(osRepository, clienteRepository, veiculoRepository)
            .executar(clienteId, veiculoId);

        assertThat(resultado.getStatus()).isEqualTo(StatusOS.RECEBIDA);
        verify(osRepository).salvar(any());
    }

    @Test
    void criarOS_deveLancarExcecaoSeClienteNaoEncontrado() {
        var clienteId = UUID.randomUUID();
        when(clienteRepository.buscarPorId(clienteId)).thenReturn(Optional.empty());

        var randomVeiculoId = UUID.randomUUID();
        var uc = new CriarOrdemServicoUseCase(osRepository, clienteRepository, veiculoRepository);
        assertThatThrownBy(() -> uc.executar(clienteId, randomVeiculoId))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void criarOS_deveLancarExcecaoSeVeiculoNaoPertenceAoCliente() {
        var clienteId = UUID.randomUUID();
        var outroClienteId = UUID.randomUUID();
        var veiculoId = UUID.randomUUID();
        var cliente = new Cliente(clienteId, new CpfCnpj("529.982.247-25"), "João", "j@e.com", "11999");
        var veiculoDeOutro = new Veiculo(veiculoId, new Placa("ABC-1234"), "Honda", "Civic", 2021, outroClienteId);

        when(clienteRepository.buscarPorId(clienteId)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.buscarPorId(veiculoId)).thenReturn(Optional.of(veiculoDeOutro));

        var uc = new CriarOrdemServicoUseCase(osRepository, clienteRepository, veiculoRepository);
        assertThatThrownBy(() -> uc.executar(clienteId, veiculoId))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("não pertence");
    }

    // ── GerarOrcamentoUseCase ─────────────────────────────────────────────

    @Test
    void gerarOrcamento_deveGerarENotificar() {
        var itemServico = new ItemServico(UUID.randomUUID(), UUID.randomUUID(), "Troca óleo",
            new BigDecimal("120.00"), 1);
        var os = OrdemServico.reconstituir()
            .id(UUID.randomUUID()).clienteId(UUID.randomUUID()).veiculoId(UUID.randomUUID())
            .status(StatusOS.EM_DIAGNOSTICO).itensServico(List.of(itemServico))
            .dataAbertura(LocalDateTime.now()).build();

        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(osRepository.salvar(os)).thenReturn(os);

        new GerarOrcamentoUseCase(osRepository, notificacaoService).executar(os.getId());

        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        verify(notificacaoService).notificarOrcamentoPendente(any(), any(), any());
    }

    @Test
    void gerarOrcamento_deveLancarExcecaoSeNaoEncontrada() {
        var id = UUID.randomUUID();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());

        var uc = new GerarOrcamentoUseCase(osRepository, notificacaoService);
        assertThatThrownBy(() -> uc.executar(id))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ── AdicionarServicoAOSUseCase ────────────────────────────────────────

    @Test
    void adicionarServico_deveAdicionarComSnapshotDePreco() {
        var os = osComStatus(StatusOS.EM_DIAGNOSTICO);
        var servicoId = UUID.randomUUID();
        var servico = new Servico(servicoId, "Alinhamento", "Alinhamento 4 rodas",
            new BigDecimal("80.00"), new BigDecimal("1.0"));

        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(servicoRepository.buscarPorId(servicoId)).thenReturn(Optional.of(servico));
        when(osRepository.salvar(os)).thenReturn(os);

        new AdicionarServicoAOSUseCase(osRepository, servicoRepository).executar(os.getId(), servicoId, 1);

        assertThat(os.getItensServico()).hasSize(1);
        assertThat(os.getItensServico().get(0).getPrecoSnapshot()).isEqualByComparingTo("80.00");
        verify(osRepository).salvar(os);
    }

    @Test
    void adicionarServico_deveLancarExcecaoSeOSNaoEncontrada() {
        var id = UUID.randomUUID();
        when(osRepository.buscarPorId(id)).thenReturn(Optional.empty());

        var randomServicoId = UUID.randomUUID();
        var uc = new AdicionarServicoAOSUseCase(osRepository, servicoRepository);
        assertThatThrownBy(() -> uc.executar(id, randomServicoId, 1))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void adicionarServico_deveLancarExcecaoSeServicoNaoEncontrado() {
        var os = osComStatus(StatusOS.EM_DIAGNOSTICO);
        var servicoId = UUID.randomUUID();
        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
        when(servicoRepository.buscarPorId(servicoId)).thenReturn(Optional.empty());

        var osId = os.getId();
        var uc = new AdicionarServicoAOSUseCase(osRepository, servicoRepository);
        assertThatThrownBy(() -> uc.executar(osId, servicoId, 1))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
