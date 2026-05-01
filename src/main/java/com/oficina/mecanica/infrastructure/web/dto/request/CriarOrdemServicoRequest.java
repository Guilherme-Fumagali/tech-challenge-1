package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CriarOrdemServicoRequest(
    @NotNull UUID clienteId,
    @NotNull UUID veiculoId
) {}
