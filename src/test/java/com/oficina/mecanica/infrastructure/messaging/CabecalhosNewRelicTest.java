package com.oficina.mecanica.infrastructure.messaging;

import com.newrelic.api.agent.HeaderType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CabecalhosNewRelicTest {

    @Test
    void copiaApenasOsCabecalhosDeRastreamento() {
        var cabecalhos = new CabecalhosNewRelic(Map.of(
            "traceparent", "00-abc-def-01", "tracestate", "nr=1", "newrelic", "eyJ2Ijp9",
            "tipo", "DiagnosticoIniciado", "id", 42));

        assertThat(cabecalhos.getHeaderType()).isEqualTo(HeaderType.MESSAGE);
        assertThat(cabecalhos.getHeaderNames()).containsExactlyInAnyOrder("traceparent", "tracestate", "newrelic");
        assertThat(cabecalhos.getHeader("traceparent")).isEqualTo("00-abc-def-01");
        assertThat(cabecalhos.getHeaders("tracestate")).containsExactly("nr=1");
        assertThat(cabecalhos.getHeaders("ausente")).isEmpty();
        assertThat(cabecalhos.containsHeader("tipo")).isFalse();
    }

    @Test
    void agenteGravaCabecalhosQueViramAtributosDaMensagem() {
        var cabecalhos = new CabecalhosNewRelic(null);

        cabecalhos.setHeader("traceparent", "00-1-2-01");
        cabecalhos.addHeader("newrelic", "payload");

        assertThat(cabecalhos.comoMapa()).containsEntry("traceparent", "00-1-2-01").containsEntry("newrelic", "payload");
        assertThat(new CabecalhosNewRelic().comoMapa()).isEmpty();
    }
}
