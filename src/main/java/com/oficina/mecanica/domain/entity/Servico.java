package com.oficina.mecanica.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Servico {

    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal precoUnitario;
    private BigDecimal tempoEstimadoHoras;

    public void atualizar(String nome, String descricao, BigDecimal preco, BigDecimal tempoHoras) {
        this.nome = nome;
        this.descricao = descricao;
        this.precoUnitario = preco;
        this.tempoEstimadoHoras = tempoHoras;
    }
}
