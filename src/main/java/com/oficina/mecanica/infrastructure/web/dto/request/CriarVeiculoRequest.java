package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CriarVeiculoRequest(
    @NotBlank String placa,
    @NotBlank String marca,
    @NotBlank String modelo,
    @Positive int anoFabricacao,
    @NotNull UUID clienteId
) {}
