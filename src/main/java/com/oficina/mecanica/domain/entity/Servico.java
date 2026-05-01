package com.oficina.mecanica.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class Servico {

    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal precoUnitario;
    private BigDecimal tempoEstimadoHoras;

    public Servico(UUID id, String nome, String descricao, BigDecimal precoUnitario, BigDecimal tempoEstimadoHoras) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.precoUnitario = precoUnitario;
        this.tempoEstimadoHoras = tempoEstimadoHoras;
    }

    public UUID getId()                    { return id; }
    public String getNome()                { return nome; }
    public String getDescricao()           { return descricao; }
    public BigDecimal getPrecoUnitario()   { return precoUnitario; }
    public BigDecimal getTempoEstimadoHoras() { return tempoEstimadoHoras; }

    public void atualizar(String nome, String descricao, BigDecimal preco, BigDecimal tempoHoras) {
        this.nome = nome;
        this.descricao = descricao;
        this.precoUnitario = preco;
        this.tempoEstimadoHoras = tempoHoras;
    }
}
