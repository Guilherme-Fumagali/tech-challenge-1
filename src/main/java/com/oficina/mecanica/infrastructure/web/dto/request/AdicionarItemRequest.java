package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdicionarItemRequest(
    @NotNull UUID itemId,
    @Min(1) int quantidade
) {}
