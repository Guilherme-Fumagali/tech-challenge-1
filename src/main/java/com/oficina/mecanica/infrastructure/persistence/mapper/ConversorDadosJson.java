package com.oficina.mecanica.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConversorDadosJson {

    private static final TypeReference<Map<String, Object>> MAPA = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ConversorDadosJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    public String paraJson(Map<String, Object> dados) {
        if (dados == null || dados.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(dados);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Dados do comando não serializáveis.", e);
        }
    }

    public Map<String, Object> deJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAPA);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Dados do comando gravados em formato inválido.", e);
        }
    }
}
