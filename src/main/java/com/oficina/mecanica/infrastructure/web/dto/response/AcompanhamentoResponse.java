package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.application.usecase.ordemservico.Acompanhamento;

import java.util.List;
import java.util.Map;

public record AcompanhamentoResponse(
    OrdemServicoResponse ordemServico,
    List<HistoricoStatusResponse> historico,
    Map<String, Object> orcamento,
    Map<String, Object> execucao,
    List<String> fontesIndisponiveis
) {
    public static AcompanhamentoResponse from(Acompanhamento acompanhamento) {
        return new AcompanhamentoResponse(
            OrdemServicoResponse.from(acompanhamento.ordemServico()),
            acompanhamento.historico().stream().map(HistoricoStatusResponse::from).toList(),
            acompanhamento.orcamento(),
            acompanhamento.execucao(),
            acompanhamento.fontesIndisponiveis());
    }
}
