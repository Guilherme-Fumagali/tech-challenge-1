package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.domain.entity.Servico;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponse(
    UUID id,
    String nome,
    String descricao,
    BigDecimal precoUnitario,
    BigDecimal tempoEstimadoHoras
) {
    public static ServicoResponse from(Servico s) {
        return new ServicoResponse(s.getId(), s.getNome(), s.getDescricao(),
            s.getPrecoUnitario(), s.getTempoEstimadoHoras());
    }
}
