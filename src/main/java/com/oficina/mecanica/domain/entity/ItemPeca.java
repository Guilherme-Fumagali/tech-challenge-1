package com.oficina.mecanica.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemPeca {

    private UUID id;
    private UUID pecaId;
    private String nomePeca;
    private BigDecimal precoSnapshot;
    private int quantidade;

    public ItemPeca(UUID id, UUID pecaId, String nomePeca, BigDecimal precoSnapshot, int quantidade) {
        this.id = id;
        this.pecaId = pecaId;
        this.nomePeca = nomePeca;
        this.precoSnapshot = precoSnapshot;
        this.quantidade = quantidade;
    }

    public BigDecimal getSubtotal() {
        return precoSnapshot.multiply(BigDecimal.valueOf(quantidade));
    }

    public UUID getId()               { return id; }
    public UUID getPecaId()           { return pecaId; }
    public String getNomePeca()       { return nomePeca; }
    public BigDecimal getPrecoSnapshot() { return precoSnapshot; }
    public int getQuantidade()        { return quantidade; }
}
