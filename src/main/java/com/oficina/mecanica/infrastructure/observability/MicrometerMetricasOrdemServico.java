package com.oficina.mecanica.infrastructure.observability;

import com.oficina.mecanica.application.port.MetricasOrdemServico;
import com.oficina.mecanica.domain.entity.TransicaoOS;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class MicrometerMetricasOrdemServico implements MetricasOrdemServico {

    private static final Logger log = LoggerFactory.getLogger(MicrometerMetricasOrdemServico.class);

    static final String OS_ABERTAS = "oficina.os.abertas";
    static final String OS_TRANSICOES = "oficina.os.transicoes";
    static final String OS_DURACAO_STATUS = "oficina.os.duracao_status";
    static final String INTEGRACAO_FALHAS = "oficina.integracao.falhas";

    private final MeterRegistry registry;

    public MicrometerMetricasOrdemServico(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void registrarAbertura(UUID ordemServicoId) {
        registry.counter(OS_ABERTAS, "origem", "api").increment();
        log.atInfo()
            .addKeyValue("os.id", ordemServicoId)
            .log("Ordem de serviço {} aberta", ordemServicoId);
    }

    @Override
    public void registrarTransicao(TransicaoOS t) {
        registry.counter(OS_TRANSICOES,
            "status_destino", t.destino().name(),
            "resultado", "sucesso").increment();

        registry.timer(OS_DURACAO_STATUS,
                "status_origem", t.origem().name(),
                "status_destino", t.destino().name())
            .record(t.duracaoNoStatusOrigem().toMillis(), TimeUnit.MILLISECONDS);

        log.atInfo()
            .addKeyValue("os.id", t.ordemServicoId())
            .addKeyValue("os.status_origem", t.origem())
            .addKeyValue("os.status_destino", t.destino())
            .log("Ordem de serviço {} passou de {} para {}", t.ordemServicoId(), t.origem(), t.destino());
    }

    @Override
    public void registrarFalhaTransicao(String motivo) {
        registry.counter(OS_TRANSICOES,
            "status_destino", "desconhecido",
            "resultado", "falha",
            "motivo", motivo).increment();
    }

    @Override
    public void registrarFalhaIntegracao(String integracao, String motivo) {
        registry.counter(INTEGRACAO_FALHAS,
            "integracao", integracao,
            "motivo", motivo).increment();
    }
}
