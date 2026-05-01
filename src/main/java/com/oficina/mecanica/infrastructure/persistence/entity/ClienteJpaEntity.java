package com.oficina.mecanica.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "clientes", uniqueConstraints = @UniqueConstraint(columnNames = "cpf_cnpj"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ClienteJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "cpf_cnpj", nullable = false, length = 14)
    private String cpfCnpj;

    @Column(nullable = false)
    private String nome;

    @Column
    private String email;

    @Column
    private String telefone;
}
