package com.oficina.mecanica.application;

import com.oficina.mecanica.application.port.ComandoAEnviar;
import com.oficina.mecanica.application.port.MetricasSaga;
import com.oficina.mecanica.application.port.PublicadorMensagens;
import com.oficina.mecanica.application.usecase.saga.ConfiguracaoSaga;
import com.oficina.mecanica.application.usecase.saga.VerificarPrazosSagaUseCase;
import com.oficina.mecanica.domain.repository.SagaOrdemServicoRepository;
import com.oficina.mecanica.domain.saga.DadosSagaOrdemServico;
import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.SagaOrdemServico;
import com.oficina.mecanica.domain.saga.TipoComando;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificarPrazosSagaUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-10-30T14:00:00Z");
    private static final Duration PRAZO = Duration.ofMinutes(2);

    @Mock SagaOrdemServicoRepository sagaRepository;
    @Mock PublicadorMensagens publicador;
    @Mock MetricasSaga metricas;

    VerificarPrazosSagaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerificarPrazosSagaUseCase(sagaRepository, publicador, metricas, new UnidadeDeTrabalhoImediata(),
            new ConfiguracaoSaga(PRAZO, 3, 20), Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void reenviaOUltimoComandoComNovaMensagem() {
        var saga = sagaVencida(1, TipoComando.GERAR_ORCAMENTO);
        when(sagaRepository.reivindicarComPrazoVencido(AGORA, 20)).thenReturn(List.of(saga));

        var tratadas = useCase.executar();

        assertThat(tratadas).isEqualTo(1);
        var comando = ArgumentCaptor.forClass(ComandoAEnviar.class);
        verify(publicador).publicar(comando.capture());
        assertThat(comando.getValue().tipo()).isEqualTo(TipoComando.GERAR_ORCAMENTO);
        assertThat(comando.getValue().dados()).containsEntry("clienteId", "c-1");
        assertThat(comando.getValue().sagaId()).isEqualTo(saga.getId());
        assertThat(saga.getTentativasReenvio()).isEqualTo(2);
        assertThat(saga.getPrazoResposta()).isEqualTo(AGORA.plus(PRAZO));
        verify(sagaRepository).salvar(saga);
        verify(metricas).registrarReenvio(saga.getId(), TipoComando.GERAR_ORCAMENTO);
    }

    @Test
    void acimaDoLimiteDeReenviosASagaVaiParaFalha() {
        var saga = sagaVencida(3, TipoComando.INICIAR_REPARO);
        when(sagaRepository.reivindicarComPrazoVencido(AGORA, 20)).thenReturn(List.of(saga));

        useCase.executar();

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.FALHA);
        verifyNoInteractions(publicador);
        verify(sagaRepository).salvar(saga);
        verify(metricas).registrarFalha(eq(saga.getId()), eq(EstadoSaga.AGUARDANDO_FILA_REPARO), anyString());
    }

    @Test
    void prazoSemComandoRegistradoVaiParaFalha() {
        var saga = SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder().id(UUID.randomUUID())
            .osId(UUID.randomUUID()).estado(EstadoSaga.AGUARDANDO_ORCAMENTO).prazoResposta(AGORA.minusSeconds(1))
            .criadaEm(AGORA).build());
        when(sagaRepository.reivindicarComPrazoVencido(AGORA, 20)).thenReturn(List.of(saga));

        useCase.executar();

        assertThat(saga.getEstado()).isEqualTo(EstadoSaga.FALHA);
        verifyNoInteractions(publicador);
    }

    @Test
    void semSagasVencidasNadaAcontece() {
        when(sagaRepository.reivindicarComPrazoVencido(AGORA, 20)).thenReturn(List.of());

        assertThat(useCase.executar()).isZero();
        verifyNoInteractions(publicador, metricas);
    }

    private static SagaOrdemServico sagaVencida(int tentativas, TipoComando comando) {
        var estado = comando == TipoComando.GERAR_ORCAMENTO ? EstadoSaga.AGUARDANDO_ORCAMENTO
            : EstadoSaga.AGUARDANDO_FILA_REPARO;
        return SagaOrdemServico.reconstituir(DadosSagaOrdemServico.builder().id(UUID.randomUUID())
            .osId(UUID.randomUUID()).estado(estado).ultimoComandoTipo(comando)
            .ultimoComandoDados(Map.of("clienteId", "c-1")).prazoResposta(AGORA.minusSeconds(1))
            .tentativasReenvio(tentativas).criadaEm(AGORA.minusSeconds(600)).atualizadaEm(AGORA.minusSeconds(130))
            .build());
    }
}
