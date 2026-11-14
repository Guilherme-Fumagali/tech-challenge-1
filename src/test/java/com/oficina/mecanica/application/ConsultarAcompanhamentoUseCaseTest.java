package com.oficina.mecanica.application;

import com.oficina.mecanica.application.port.ConsultaExecucao;
import com.oficina.mecanica.application.port.ConsultaOrcamento;
import com.oficina.mecanica.application.usecase.ordemservico.ConsultarAcompanhamentoUseCase;
import com.oficina.mecanica.application.usecase.ordemservico.ConsultarStatusOSUseCase;
import com.oficina.mecanica.domain.entity.DadosOrdemServico;
import com.oficina.mecanica.domain.entity.OrdemServico;
import com.oficina.mecanica.domain.exception.RecursoNaoEncontradoException;
import com.oficina.mecanica.domain.repository.HistoricoStatusRepository;
import com.oficina.mecanica.domain.repository.OrdemServicoRepository;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultarAcompanhamentoUseCaseTest {

    @Mock OrdemServicoRepository osRepository;
    @Mock HistoricoStatusRepository historicoRepository;
    @Mock ConsultaOrcamento consultaOrcamento;
    @Mock ConsultaExecucao consultaExecucao;

    ConsultarAcompanhamentoUseCase useCase;
    OrdemServico os;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarAcompanhamentoUseCase(new ConsultarStatusOSUseCase(osRepository), historicoRepository,
            consultaOrcamento, consultaExecucao);
        os = OrdemServico.reconstituir(DadosOrdemServico.builder().id(UUID.randomUUID()).clienteId(UUID.randomUUID())
            .veiculoId(UUID.randomUUID()).status(StatusOS.AGUARDANDO_PAGAMENTO).dataAbertura(LocalDateTime.now()).build());
        when(osRepository.buscarPorId(os.getId())).thenReturn(Optional.of(os));
    }

    @Test
    void reuneAsTresFontes() {
        when(historicoRepository.listarPorOrdemServico(os.getId())).thenReturn(List.of());
        when(consultaOrcamento.buscarPorOrdemServico(os.getId(), "Bearer t"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(Map.of("status", "APROVADO"))));
        when(consultaExecucao.buscarPorOrdemServico(os.getId(), "Bearer t"))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        var resultado = useCase.executar(os.getId(), null, "Bearer t");

        assertThat(resultado.ordemServico()).isSameAs(os);
        assertThat(resultado.orcamento()).containsEntry("status", "APROVADO");
        assertThat(resultado.execucao()).isNull();
        assertThat(resultado.fontesIndisponiveis()).isEmpty();
    }

    @Test
    void fonteQueFalhaViraNulaEIndisponivel() {
        when(consultaOrcamento.buscarPorOrdemServico(os.getId(), null))
            .thenReturn(CompletableFuture.failedFuture(new TimeoutException()));
        when(consultaExecucao.buscarPorOrdemServico(os.getId(), null))
            .thenThrow(new IllegalStateException("bulkhead cheio"));

        var resultado = useCase.executar(os.getId(), os.getClienteId(), null);

        assertThat(resultado.orcamento()).isNull();
        assertThat(resultado.execucao()).isNull();
        assertThat(resultado.fontesIndisponiveis()).containsExactly("billing", "execution");
    }

    @Test
    void clienteNaoAcompanhaOrdemDeOutroCliente() {
        var outroCliente = UUID.randomUUID();
        var osId = os.getId();

        assertThatThrownBy(() -> useCase.executar(osId, outroCliente, null))
            .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(consultaOrcamento, consultaExecucao);
    }
}
