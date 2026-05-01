package com.oficina.mecanica.infrastructure.web.dto.response;

import com.oficina.mecanica.domain.entity.Peca;

import java.math.BigDecimal;
import java.util.UUID;

public record PecaResponse(
    UUID id,
    String nome,
    String descricao,
    BigDecimal precoUnitario,
    int quantidadeEstoque,
    int estoqueMinimo,
    boolean abaixoDoMinimo
) {
    public static PecaResponse from(Peca p) {
        return new PecaResponse(p.getId(), p.getNome(), p.getDescricao(),
            p.getPrecoUnitario(), p.getQuantidadeEstoque(), p.getEstoqueMinimo(),
            p.estaBaixoDoMinimo());
    }
}
