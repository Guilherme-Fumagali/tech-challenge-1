package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistoricoStatus(
    UUID ordemServicoId,
    StatusOS statusOrigem,
    StatusOS statusDestino,
    String evento,
    LocalDateTime ocorridoEm
) {}
