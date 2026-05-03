package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AtualizarVeiculoRequest(
    @NotBlank String marca,
    @NotBlank String modelo,
    @Min(1900) @Max(2100) int anoFabricacao
) {}
