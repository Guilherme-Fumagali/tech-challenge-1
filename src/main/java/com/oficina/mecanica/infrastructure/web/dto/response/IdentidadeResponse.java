package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.application.usecase.identidade.Identidade;

import java.util.UUID;

public record IdentidadeResponse(UUID id, String nome, String email, String status) {

    public static IdentidadeResponse from(Identidade identidade) {
        return new IdentidadeResponse(identidade.id(), identidade.nome(), identidade.email(), identidade.status());
    }
}
