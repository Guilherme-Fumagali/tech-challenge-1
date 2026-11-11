package com.oficina.mecanica.infrastructure.messaging;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CabecalhosNewRelic implements Headers {

    static final Set<String> NOMES = Set.of("newrelic", "traceparent", "tracestate");

    private final Map<String, String> valores = new LinkedHashMap<>();

    public CabecalhosNewRelic() {
    }

    public CabecalhosNewRelic(Map<String, ?> origem) {
        if (origem != null) {
            origem.forEach((nome, valor) -> {
                if (NOMES.contains(nome) && valor instanceof String texto) {
                    valores.put(nome, texto);
                }
            });
        }
    }

    @Override
    public HeaderType getHeaderType() {
        return HeaderType.MESSAGE;
    }

    @Override
    public String getHeader(String nome) {
        return valores.get(nome);
    }

    @Override
    public Collection<String> getHeaders(String nome) {
        var valor = valores.get(nome);
        return valor == null ? List.of() : List.of(valor);
    }

    @Override
    public void setHeader(String nome, String valor) {
        valores.put(nome, valor);
    }

    @Override
    public void addHeader(String nome, String valor) {
        valores.put(nome, valor);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return List.copyOf(valores.keySet());
    }

    @Override
    public boolean containsHeader(String nome) {
        return valores.containsKey(nome);
    }

    public Map<String, String> comoMapa() {
        return Map.copyOf(valores);
    }
}
