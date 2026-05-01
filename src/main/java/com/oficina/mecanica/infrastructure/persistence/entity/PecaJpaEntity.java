package com.oficina.mecanica.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pecas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PecaJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column
    private String descricao;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;
}
