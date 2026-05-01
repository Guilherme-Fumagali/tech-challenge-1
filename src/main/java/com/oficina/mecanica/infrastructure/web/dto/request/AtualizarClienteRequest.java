package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AtualizarClienteRequest(
    @NotBlank String nome,
    String email,
    String telefone
) {}
