package com.oficina.mecanica.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AprovarOrcamentoExternoRequest(
    @NotBlank String token,
    @NotNull DecisaoAprovacaoExterna decisao
) {}
