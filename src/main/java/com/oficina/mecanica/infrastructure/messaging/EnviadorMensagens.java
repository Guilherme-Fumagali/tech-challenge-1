package com.oficina.mecanica.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TransportType;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EnviadorMensagens {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {};

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;
    private final PropriedadesMensageria propriedades;

    public EnviadorMensagens(SqsTemplate sqsTemplate, ObjectMapper objectMapper,
                             PropriedadesMensageria propriedades) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
        this.propriedades = propriedades;
    }

    @Trace(dispatcher = true)
    public void enviar(OutboxJpaEntity linha) {
        NewRelic.setTransactionName("Mensageria", "Relay/" + linha.getTipo());
        var transacao = NewRelic.getAgent().getTransaction();
        transacao.acceptDistributedTraceHeaders(TransportType.Queue, new CabecalhosNewRelic(ler(linha.getCabecalhos())));
        var cabecalhos = new CabecalhosNewRelic();
        transacao.insertDistributedTraceHeaders(cabecalhos);

        var sagaId = String.valueOf(ler(linha.getPayload()).get("sagaId"));
        NewRelic.addCustomParameter(ContextoMensagem.OS_ID, linha.getOsId().toString());
        NewRelic.addCustomParameter(ContextoMensagem.SAGA_ID, sagaId);
        NewRelic.addCustomParameter(ContextoMensagem.MENSAGEM_TIPO, linha.getTipo());
        MDC.put(ContextoMensagem.OS_ID, linha.getOsId().toString());
        MDC.put(ContextoMensagem.SAGA_ID, sagaId);
        MDC.put(ContextoMensagem.MENSAGEM_ID, linha.getId().toString());
        try {
            sqsTemplate.send(opcoes -> opcoes
                .queue(propriedades.nomeDaFila(linha.getFila()))
                .payload(linha.getPayload())
                .header(ContextoMensagem.ATRIBUTO_TIPO, linha.getTipo())
                .header(ContextoMensagem.ATRIBUTO_VERSAO, String.valueOf(linha.getVersao()))
                .headers(Map.copyOf(cabecalhos.comoMapa())));
        } finally {
            ContextoMensagem.limparMdc();
        }
    }

    private Map<String, Object> ler(String json) {
        try {
            return objectMapper.readValue(json, MAPA);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Conteúdo inválido na linha do outbox.", e);
        }
    }
}
