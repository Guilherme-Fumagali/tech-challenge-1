package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.OrdemServico;

import java.util.UUID;

public record AberturaOrdemServico(OrdemServico ordemServico, UUID sagaId) {}
