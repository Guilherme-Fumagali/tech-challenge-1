package com.oficina.mecanica.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.oficina.mecanica.application.usecase.saga.ProcessarEventoSagaUseCase;
import com.oficina.mecanica.application.usecase.saga.ResultadoConsumo;
import com.oficina.mecanica.domain.saga.TipoEvento;
import com.oficina.mecanica.infrastructure.observability.MetricasMensageria;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumidorEventosOsTest {

    @Mock ProcessarEventoSagaUseCase processarEvento;

    SimpleMeterRegistry registry;
    ConsumidorEventosOs consumidor;
    UUID osId;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        consumidor = new ConsumidorEventosOs(new ObjectMapper().registerModule(new JavaTimeModule()),
            new TradutorEventos(), processarEvento, new MetricasMensageria(registry));
        osId = UUID.randomUUID();
    }

    @Test
    void eventoConhecidoEhEntregueAoCasoDeUsoEContado() {
        when(processarEvento.executar(eq(osId), any())).thenReturn(ResultadoConsumo.APLICADA);

        consumidor.receber(MessageBuilder.withPayload(corpo("DiagnosticoIniciado"))
            .setHeader("traceparent", "00-1-2-01").build());

        verify(processarEvento).executar(eq(osId), argThat(evento -> evento.tipo() == TipoEvento.DIAGNOSTICO_INICIADO));
        assertThat(registry.counter("oficina.mensageria.consumidas", "tipo", "DiagnosticoIniciado",
            "resultado", "aplicada").count()).isEqualTo(1);
        assertThat(MDC.get("os.id")).isNull();
    }

    @Test
    void duplicataEhContadaComoDuplicada() {
        when(processarEvento.executar(eq(osId), any())).thenReturn(ResultadoConsumo.DUPLICADA);

        consumidor.processar(corpo("OrcamentoGerado"), Map.of());

        assertThat(registry.counter("oficina.mensageria.consumidas", "tipo", "OrcamentoGerado",
            "resultado", "duplicada").count()).isEqualTo(1);
    }

    @Test
    void tipoDesconhecidoEhRejeitado() {
        when(processarEvento.rejeitar(eq(osId), any(), eq("EventoNovo"), any())).thenReturn(ResultadoConsumo.REJEITADA);

        var resultado = consumidor.processar(corpo("EventoNovo"), Map.of());

        assertThat(resultado).isEqualTo(ResultadoConsumo.REJEITADA);
        verify(processarEvento, never()).executar(any(), any());
    }

    @Test
    void falhaNoProcessamentoEhContadaEPropagada() {
        when(processarEvento.executar(eq(osId), any())).thenThrow(new IllegalStateException("banco fora"));
        var corpo = corpo("PagamentoAprovado");
        var cabecalhos = Map.<String, Object>of();

        assertThatThrownBy(() -> consumidor.processar(corpo, cabecalhos)).isInstanceOf(IllegalStateException.class);
        assertThat(registry.counter("oficina.mensageria.consumidas", "tipo", "PagamentoAprovado",
            "resultado", "erro").count()).isEqualTo(1);
        assertThat(MDC.get("saga.id")).isNull();
    }

    @Test
    void corpoForaDoEnvelopeEhRecusado() {
        var semIdentificadores = "{\"tipo\":\"DiagnosticoIniciado\"}";
        var cabecalhos = Map.<String, Object>of();

        assertThatThrownBy(() -> consumidor.processar("nao-e-json", cabecalhos))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumidor.processar(semIdentificadores, cabecalhos))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(processarEvento);
    }

    private String corpo(String tipo) {
        return """
            {"mensagemId":"%s","tipo":"%s","versao":1,"sagaId":"%s","osId":"%s","causadaPor":null,
             "origem":"execution-service","ocorridoEm":"2026-10-30T14:05:11.482Z","dados":{},"campoNovo":true}
            """.formatted(UUID.randomUUID(), tipo, UUID.randomUUID(), osId);
    }
}
