package com.oficina.mecanica.infrastructure.observability;

import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.MotivoCompensacao;
import com.oficina.mecanica.domain.saga.TipoComando;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerMetricasSagaTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerMetricasSaga metricas = new MicrometerMetricasSaga(registry);

    @Test
    void contadoresUsamOsNomesEAtributosDaSpec14() {
        var sagaId = UUID.randomUUID();

        metricas.registrarInicio(sagaId);
        metricas.registrarConclusao(sagaId);
        metricas.registrarCompensacao(sagaId, MotivoCompensacao.PAGAMENTO_RECUSADO);
        metricas.registrarFalha(sagaId, EstadoSaga.COMPENSANDO, "rejeitado");
        metricas.registrarReenvio(sagaId, TipoComando.GERAR_ORCAMENTO);

        assertThat(registry.counter("oficina.saga.iniciadas").count()).isEqualTo(1);
        assertThat(registry.counter("oficina.saga.concluidas").count()).isEqualTo(1);
        assertThat(registry.counter("oficina.saga.compensadas", "motivo", "PAGAMENTO_RECUSADO").count()).isEqualTo(1);
        assertThat(registry.counter("oficina.saga.falhas", "estado", "COMPENSANDO").count()).isEqualTo(1);
        assertThat(registry.counter("oficina.saga.reenvios", "comando", "GerarOrcamento").count()).isEqualTo(1);
    }

    @Test
    void permanenciaNoEstadoViraTimerPorEstado() {
        metricas.registrarPermanencia(EstadoSaga.AGUARDANDO_APROVACAO, Duration.ofMinutes(3));
        metricas.registrarPermanencia(EstadoSaga.AGUARDANDO_APROVACAO, null);

        var timer = registry.timer("oficina.saga.etapa.duracao", "estado", "AGUARDANDO_APROVACAO");
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MINUTES)).isEqualTo(3.0);
    }
}
