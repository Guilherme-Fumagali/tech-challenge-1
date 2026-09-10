package com.oficina.mecanica.infrastructure.web.dto.request;

import com.oficina.mecanica.domain.valueobject.StatusCliente;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusClienteRequest(
    @NotNull(message = "status é obrigatório")
    StatusCliente status
) {}
