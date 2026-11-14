package com.oficina.mecanica.application.usecase.ordemservico;

import com.oficina.mecanica.domain.entity.HistoricoStatus;
import com.oficina.mecanica.domain.entity.OrdemServico;

import java.util.List;
import java.util.Map;

public record Acompanhamento(
    OrdemServico ordemServico,
    List<HistoricoStatus> historico,
    Map<String, Object> orcamento,
    Map<String, Object> execucao,
    List<String> fontesIndisponiveis
) {}
