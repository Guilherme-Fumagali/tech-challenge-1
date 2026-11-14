package com.oficina.mecanica.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oficina.mecanica.application.port.ConsultaOrcamento;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class BillingRestClient implements ConsultaOrcamento {

    public static final String FONTE = "billing";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    public BillingRestClient(@Qualifier(FONTE) RestClient restClient, ObjectMapper objectMapper,
                             @Qualifier(IntegracoesConfig.EXECUTOR) Executor executor) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    @Override
    @CircuitBreaker(name = FONTE)
    @TimeLimiter(name = FONTE)
    @Bulkhead(name = FONTE)
    public CompletableFuture<Optional<Map<String, Object>>> buscarPorOrdemServico(UUID osId, String autorizacao) {
        return CompletableFuture.supplyAsync(() -> consultar(osId, autorizacao), executor);
    }

    private Optional<Map<String, Object>> consultar(UUID osId, String autorizacao) {
        try {
            var corpo = restClient.get()
                .uri(uri -> uri.path("/api/orcamentos").queryParam("osId", osId).build())
                .headers(cabecalhos -> {
                    if (autorizacao != null) {
                        cabecalhos.set(HttpHeaders.AUTHORIZATION, autorizacao);
                    }
                })
                .retrieve()
                .body(JsonNode.class);
            return RespostaParticipante.normalizar(corpo, objectMapper);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
