package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CriarServicoRequest(
    @NotBlank String nome,
    String descricao,
    @NotNull @DecimalMin("0.01") BigDecimal precoUnitario,
    @NotNull @DecimalMin("0.01") BigDecimal tempoEstimadoHoras
) {}
