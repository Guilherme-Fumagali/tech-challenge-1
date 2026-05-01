package com.oficina.mecanica.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "veiculos", uniqueConstraints = @UniqueConstraint(columnNames = "placa"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VeiculoJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 8)
    private String placa;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(name = "ano_fabricacao", nullable = false)
    private int anoFabricacao;

    @Column(name = "cliente_id", nullable = false, columnDefinition = "uuid")
    private UUID clienteId;
}
