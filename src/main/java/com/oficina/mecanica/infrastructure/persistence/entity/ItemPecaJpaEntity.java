package com.oficina.mecanica.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "itens_peca")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ItemPecaJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServicoJpaEntity ordemServico;

    @Column(name = "peca_id", nullable = false, columnDefinition = "uuid")
    private UUID pecaId;

    @Column(name = "nome_peca", nullable = false)
    private String nomePeca;

    @Column(name = "preco_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoSnapshot;

    @Column(nullable = false)
    private int quantidade;
}
