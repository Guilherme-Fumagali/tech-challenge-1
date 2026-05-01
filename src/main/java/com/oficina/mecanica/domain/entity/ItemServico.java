package com.oficina.mecanica.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ItemServico {

    private UUID id;
    private UUID servicoId;
    private String nomeServico;
    private BigDecimal precoSnapshot;
    private int quantidade;

    public BigDecimal getSubtotal() {
        return precoSnapshot.multiply(BigDecimal.valueOf(quantidade));
    }
}
