package com.oficina.mecanica.infrastructure.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensagens_processadas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MensagemProcessadaJpaEntity {

    @Id
    @Column(name = "mensagem_id", columnDefinition = "uuid")
    private UUID mensagemId;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(name = "processada_em", nullable = false)
    private LocalDateTime processadaEm;
}
