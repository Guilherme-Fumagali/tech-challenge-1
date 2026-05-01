package com.oficina.mecanica.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemServico {

    private UUID id;
    private UUID servicoId;
    private String nomeServico;
    private BigDecimal precoSnapshot;
    private int quantidade;

    public ItemServico(UUID id, UUID servicoId, String nomeServico, BigDecimal precoSnapshot, int quantidade) {
        this.id = id;
        this.servicoId = servicoId;
        this.nomeServico = nomeServico;
        this.precoSnapshot = precoSnapshot;
        this.quantidade = quantidade;
    }

    public BigDecimal getSubtotal() {
        return precoSnapshot.multiply(BigDecimal.valueOf(quantidade));
    }

    public UUID getId()               { return id; }
    public UUID getServicoId()        { return servicoId; }
    public String getNomeServico()    { return nomeServico; }
    public BigDecimal getPrecoSnapshot() { return precoSnapshot; }
    public int getQuantidade()        { return quantidade; }
}
