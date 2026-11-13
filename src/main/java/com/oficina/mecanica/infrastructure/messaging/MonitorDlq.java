package com.oficina.mecanica.infrastructure.messaging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class MonitorDlq {

    static final String METRICA = "oficina.mensageria.dlq.mensagens";

    private static final Logger log = LoggerFactory.getLogger(MonitorDlq.class);

    private final SqsAsyncClient sqs;
    private final String fila;
    private final AtomicLong mensagens = new AtomicLong();
    private volatile String urlDaFila;

    public MonitorDlq(SqsAsyncClient sqs, PropriedadesMensageria propriedades, MeterRegistry registry) {
        this.sqs = sqs;
        this.fila = propriedades.dlq();
        Gauge.builder(METRICA, mensagens, AtomicLong::get)
            .description("Mensagens aguardando na fila de mensagens mortas")
            .tag("fila", fila)
            .register(registry);
    }

    @Scheduled(fixedDelayString = "${app.mensageria.dlq-intervalo:60000}",
        initialDelayString = "${app.mensageria.dlq-atraso-inicial:5000}")
    public void atualizar() {
        try {
            if (urlDaFila == null) {
                urlDaFila = sqs.getQueueUrl(requisicao -> requisicao.queueName(fila)).join().queueUrl();
            }
            var atributos = sqs.getQueueAttributes(requisicao -> requisicao.queueUrl(urlDaFila)
                .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)).join().attributes();
            mensagens.set(Long.parseLong(atributos.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)));
        } catch (RuntimeException e) {
            log.warn("Não foi possível ler a quantidade de mensagens da fila {}: {}", fila, e.getMessage());
        }
    }

    public long mensagensNaFila() {
        return mensagens.get();
    }
}
