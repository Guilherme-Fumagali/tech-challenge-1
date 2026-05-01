package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.Min;

public record IncrementarEstoqueRequest(
    @Min(1) int quantidade
) {}
