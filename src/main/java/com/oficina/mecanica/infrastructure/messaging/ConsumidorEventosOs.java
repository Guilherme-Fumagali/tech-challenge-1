package com.oficina.mecanica.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransportType;
import com.oficina.mecanica.application.usecase.saga.ProcessarEventoSagaUseCase;
import com.oficina.mecanica.application.usecase.saga.ResultadoConsumo;
import com.oficina.mecanica.domain.saga.TipoEvento;
import com.oficina.mecanica.infrastructure.observability.MetricasMensageria;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConsumidorEventosOs {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventosOs.class);
    private static final String RESULTADO_ERRO = "erro";

    private final ObjectReader leitorDeEnvelope;
    private final TradutorEventos tradutor;
    private final ProcessarEventoSagaUseCase processarEvento;
    private final MetricasMensageria metricas;

    public ConsumidorEventosOs(ObjectMapper objectMapper, TradutorEventos tradutor,
                               ProcessarEventoSagaUseCase processarEvento, MetricasMensageria metricas) {
        this.leitorDeEnvelope = objectMapper.readerFor(MensagemEnvelope.class)
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.tradutor = tradutor;
        this.processarEvento = processarEvento;
        this.metricas = metricas;
    }

    @SqsListener(value = "${app.mensageria.filas.entrada}", maxConcurrentMessages = "5",
        maxMessagesPerPoll = "5", acknowledgementMode = "ON_SUCCESS")
    public void receber(Message<String> mensagem) {
        processar(mensagem.getPayload(), mensagem.getHeaders());
    }

    @Trace(dispatcher = true)
    public ResultadoConsumo processar(String corpo, Map<String, Object> cabecalhos) {
        var envelope = ler(corpo);
        NewRelic.setTransactionName("Mensageria", "Consumo/" + envelope.tipo());
        NewRelic.getAgent().getTransaction()
            .acceptDistributedTraceHeaders(TransportType.Queue, new CabecalhosNewRelic(cabecalhos));
        NewRelic.addCustomParameter(ContextoMensagem.OS_ID, String.valueOf(envelope.osId()));
        NewRelic.addCustomParameter(ContextoMensagem.SAGA_ID, String.valueOf(envelope.sagaId()));
        NewRelic.addCustomParameter(ContextoMensagem.MENSAGEM_TIPO, envelope.tipo());
        MDC.put(ContextoMensagem.OS_ID, String.valueOf(envelope.osId()));
        MDC.put(ContextoMensagem.SAGA_ID, String.valueOf(envelope.sagaId()));
        MDC.put(ContextoMensagem.MENSAGEM_ID, String.valueOf(envelope.mensagemId()));
        try {
            var resultado = TipoEvento.porNome(envelope.tipo())
                .map(tipo -> processarEvento.executar(envelope.osId(), tradutor.traduzir(envelope, tipo)))
                .orElseGet(() -> processarEvento.rejeitar(envelope.osId(), envelope.mensagemId(), envelope.tipo(),
                    "tipo de evento desconhecido"));
            metricas.registrarConsumo(envelope.tipo(), resultado.rotulo());
            registrarLog(envelope, resultado);
            return resultado;
        } catch (RuntimeException e) {
            metricas.registrarConsumo(envelope.tipo(), RESULTADO_ERRO);
            log.warn("Falha ao processar {} da OS {}; a mensagem volta para a fila: {}",
                envelope.tipo(), envelope.osId(), e.getMessage());
            throw e;
        } finally {
            ContextoMensagem.limparMdc();
        }
    }

    private MensagemEnvelope ler(String corpo) {
        try {
            MensagemEnvelope envelope = leitorDeEnvelope.readValue(corpo);
            if (envelope.mensagemId() == null || envelope.osId() == null || envelope.tipo() == null) {
                throw new IllegalArgumentException("Envelope sem mensagemId, osId ou tipo.");
            }
            return envelope;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Mensagem fora do formato do envelope.", e);
        }
    }

    private static void registrarLog(MensagemEnvelope envelope, ResultadoConsumo resultado) {
        if (resultado == ResultadoConsumo.DUPLICADA) {
            log.debug("Mensagem {} duplicada descartada", envelope.mensagemId());
        } else {
            log.info("Evento {} da OS {} processado: {}", envelope.tipo(), envelope.osId(), resultado.rotulo());
        }
    }
}
