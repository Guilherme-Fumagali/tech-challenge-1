package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransicaoOS(
    UUID ordemServicoId,
    StatusOS origem,
    StatusOS destino,
    Duration duracaoNoStatusOrigem,
    String evento,
    LocalDateTime ocorridaEm
) {

    public boolean alterouStatus() {
        return origem != destino;
    }
}
