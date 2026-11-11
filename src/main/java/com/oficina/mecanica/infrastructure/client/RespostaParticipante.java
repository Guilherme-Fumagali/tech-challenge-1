package com.oficina.mecanica.infrastructure.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

final class RespostaParticipante {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {};

    private RespostaParticipante() {}

    static Optional<Map<String, Object>> normalizar(JsonNode corpo, ObjectMapper objectMapper) {
        if (corpo == null || corpo.isNull()) {
            return Optional.empty();
        }
        var documento = corpo.isArray() ? corpo.path(0) : corpo;
        if (!documento.isObject()) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.convertValue(documento, MAPA));
    }
}
