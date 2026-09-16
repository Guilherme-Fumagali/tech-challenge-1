package com.oficina.mecanica.infrastructure.observability;

import com.oficina.mecanica.domain.entity.TransicaoOS;
import com.oficina.mecanica.domain.valueobject.StatusOS;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class MicrometerMetricasOrdemServicoTest {

    private SimpleMeterRegistry registry;
    private MicrometerMetricasOrdemServico metricas;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metricas = new MicrometerMetricasOrdemServico(registry);
    }

    @Test
    void aberturaDeOsAlimentaODashboardDeVolumeDiario() {
        metricas.registrarAbertura(UUID.randomUUID());
        metricas.registrarAbertura(UUID.randomUUID());

        assertThat(registry.counter("oficina.os.abertas", "origem", "api").count()).isEqualTo(2);
    }

    @Test
    void aberturaETransicaoGeramLogComIdDaOrdem(CapturedOutput saida) {
        var id = UUID.randomUUID();

        metricas.registrarAbertura(id);
        metricas.registrarTransicao(new TransicaoOS(
            id, StatusOS.RECEBIDA, StatusOS.EM_DIAGNOSTICO, Duration.ofMinutes(5)));

        assertThat(saida.getOut())
            .contains("Ordem de serviço " + id + " aberta")
            .contains("Ordem de serviço " + id + " passou de RECEBIDA para EM_DIAGNOSTICO");
    }

    @Test
    void transicaoRegistraContagemEPermanenciaNoStatusAnterior() {
        metricas.registrarTransicao(new TransicaoOS(
            UUID.randomUUID(), StatusOS.EM_DIAGNOSTICO, StatusOS.AGUARDANDO_APROVACAO,
            Duration.ofMinutes(30)));

        var contador = registry.counter("oficina.os.transicoes",
            "status_destino", "AGUARDANDO_APROVACAO", "resultado", "sucesso");
        assertThat(contador.count()).isEqualTo(1);

        var timer = registry.timer("oficina.os.duracao_status",
            "status_origem", "EM_DIAGNOSTICO", "status_destino", "AGUARDANDO_APROVACAO");
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MINUTES)).isEqualTo(30.0);
    }

    @Test
    void falhaDeTransicaoEhContadaSeparadamenteDoSucesso() {
        metricas.registrarFalhaTransicao("TransicaoInvalidaException");

        assertThat(registry.counter("oficina.os.transicoes",
            "status_destino", "desconhecido",
            "resultado", "falha",
            "motivo", "TransicaoInvalidaException").count()).isEqualTo(1);
    }

    @Test
    void falhaDeIntegracaoEhSegmentadaPorIntegracaoEMotivo() {
        metricas.registrarFalhaIntegracao("email", "montagem_mensagem");

        assertThat(registry.counter("oficina.integracao.falhas",
            "integracao", "email", "motivo", "montagem_mensagem").count()).isEqualTo(1);
    }
}
