package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CriarClienteRequest(
    @NotBlank String cpfCnpj,
    @NotBlank String nome,
    String email,
    String telefone
) {}
