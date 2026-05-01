package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.exception.EstoqueInsuficienteException;

import java.math.BigDecimal;
import java.util.UUID;

public class Peca {

    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal precoUnitario;
    private int quantidadeEstoque;
    private int estoqueMinimo;

    public Peca(UUID id, String nome, String descricao, BigDecimal precoUnitario,
                int quantidadeEstoque, int estoqueMinimo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.precoUnitario = precoUnitario;
        this.quantidadeEstoque = quantidadeEstoque;
        this.estoqueMinimo = estoqueMinimo;
    }

    public void decrementarEstoque(int quantidade) {
        if (quantidade > quantidadeEstoque) {
            throw new EstoqueInsuficienteException(nome, quantidade, quantidadeEstoque);
        }
        this.quantidadeEstoque -= quantidade;
    }

    public void incrementarEstoque(int quantidade) {
        this.quantidadeEstoque += quantidade;
    }

    public boolean estaBaixoDoMinimo() {
        return quantidadeEstoque < estoqueMinimo;
    }

    public UUID getId()                  { return id; }
    public String getNome()              { return nome; }
    public String getDescricao()         { return descricao; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public int getQuantidadeEstoque()    { return quantidadeEstoque; }
    public int getEstoqueMinimo()        { return estoqueMinimo; }

    public void atualizar(String nome, String descricao, BigDecimal preco, int estoqueMinimo) {
        this.nome = nome;
        this.descricao = descricao;
        this.precoUnitario = preco;
        this.estoqueMinimo = estoqueMinimo;
    }
}
