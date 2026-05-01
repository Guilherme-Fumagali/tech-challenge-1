package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CriarPecaRequest(
    @NotBlank String nome,
    String descricao,
    @NotNull @DecimalMin("0.01") BigDecimal precoUnitario,
    @Min(0) int quantidadeEstoque,
    @Min(0) int estoqueMinimo
) {}
