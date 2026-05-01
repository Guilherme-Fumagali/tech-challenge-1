package com.oficina.mecanica.domain.entity;

import com.oficina.mecanica.domain.exception.EstoqueInsuficienteException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Peca {

    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal precoUnitario;
    private int quantidadeEstoque;
    private int estoqueMinimo;

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

    public void atualizar(String nome, String descricao, BigDecimal preco, int estoqueMinimo) {
        this.nome = nome;
        this.descricao = descricao;
        this.precoUnitario = preco;
        this.estoqueMinimo = estoqueMinimo;
    }
}
