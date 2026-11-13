package com.oficina.mecanica.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.NewRelic;
import com.oficina.mecanica.application.port.ComandoAEnviar;
import com.oficina.mecanica.application.port.PublicadorMensagens;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
public class PublicadorOutbox implements PublicadorMensagens {

    static final int VERSAO_CONTRATO = 1;

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final PropriedadesMensageria propriedades;
    private final Clock relogio;

    public PublicadorOutbox(EntityManager entityManager, ObjectMapper objectMapper,
                            PropriedadesMensageria propriedades, Clock relogio) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    @Override
    public void publicar(ComandoAEnviar comando) {
        var agora = Instant.now(relogio).truncatedTo(ChronoUnit.MILLIS);
        var envelope = new MensagemEnvelope(comando.mensagemId(), comando.tipo().nome(), VERSAO_CONTRATO,
            comando.sagaId(), comando.osId(), comando.causadaPor(), propriedades.origem(), agora,
            objectMapper.valueToTree(comando.dados()));
        var cabecalhos = new CabecalhosNewRelic();
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(cabecalhos);

        entityManager.persist(new OutboxJpaEntity(
            comando.mensagemId(),
            propriedades.chaveDaFila(comando.tipo().destino()),
            comando.tipo().nome(),
            VERSAO_CONTRATO,
            comando.osId(),
            json(envelope),
            json(cabecalhos.comoMapa()),
            LocalDateTime.ofInstant(agora, ZoneOffset.UTC),
            null,
            0,
            null));
    }

    private String json(Object valor) {
        try {
            return objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar mensagem para o outbox.", e);
        }
    }
}
