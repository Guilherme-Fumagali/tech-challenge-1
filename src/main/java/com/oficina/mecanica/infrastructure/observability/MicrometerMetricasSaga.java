package com.oficina.mecanica.infrastructure.observability;

import com.oficina.mecanica.application.port.MetricasSaga;
import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.MotivoCompensacao;
import com.oficina.mecanica.domain.saga.TipoComando;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class MicrometerMetricasSaga implements MetricasSaga {

    static final String INICIADAS = "oficina.saga.iniciadas";
    static final String CONCLUIDAS = "oficina.saga.concluidas";
    static final String COMPENSADAS = "oficina.saga.compensadas";
    static final String FALHAS = "oficina.saga.falhas";
    static final String REENVIOS = "oficina.saga.reenvios";
    static final String DURACAO_ETAPA = "oficina.saga.etapa.duracao";

    private static final Logger log = LoggerFactory.getLogger(MicrometerMetricasSaga.class);
    private static final String SAGA_ID = "saga.id";

    private final MeterRegistry registry;

    public MicrometerMetricasSaga(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void registrarInicio(UUID sagaId) {
        registry.counter(INICIADAS).increment();
        log.atInfo().addKeyValue(SAGA_ID, sagaId).log("Saga {} iniciada", sagaId);
    }

    @Override
    public void registrarConclusao(UUID sagaId) {
        registry.counter(CONCLUIDAS).increment();
        log.atInfo().addKeyValue(SAGA_ID, sagaId).log("Saga {} concluída", sagaId);
    }

    @Override
    public void registrarCompensacao(UUID sagaId, MotivoCompensacao motivo) {
        registry.counter(COMPENSADAS, "motivo", motivo.name()).increment();
        log.atInfo().addKeyValue(SAGA_ID, sagaId).log("Saga {} compensada por {}", sagaId, motivo);
    }

    @Override
    public void registrarFalha(UUID sagaId, EstadoSaga estado, String motivo) {
        registry.counter(FALHAS, "estado", estado.name()).increment();
        log.atError().addKeyValue(SAGA_ID, sagaId)
            .log("Saga {} em FALHA a partir de {}: {}. Requer intervenção manual.", sagaId, estado, motivo);
    }

    @Override
    public void registrarReenvio(UUID sagaId, TipoComando comando) {
        registry.counter(REENVIOS, "comando", comando.nome()).increment();
        log.atWarn().addKeyValue(SAGA_ID, sagaId).log("Saga {} reenviou {} por prazo vencido", sagaId, comando.nome());
    }

    @Override
    public void registrarPermanencia(EstadoSaga estado, Duration duracao) {
        if (duracao != null) {
            registry.timer(DURACAO_ETAPA, "estado", estado.name()).record(duracao);
        }
    }
}
