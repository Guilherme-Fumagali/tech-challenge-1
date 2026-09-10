package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.valueobject.StatusOS;

import java.time.Duration;
import java.util.UUID;

public record TransicaoOS(
    UUID ordemServicoId,
    StatusOS origem,
    StatusOS destino,
    Duration duracaoNoStatusOrigem
) {}
