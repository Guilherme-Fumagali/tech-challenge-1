package com.oficina.mecanica.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ItemPeca {

    private UUID id;
    private UUID pecaId;
    private String nomePeca;
    private BigDecimal precoSnapshot;
    private int quantidade;

    public BigDecimal getSubtotal() {
        return precoSnapshot.multiply(BigDecimal.valueOf(quantidade));
    }
}
