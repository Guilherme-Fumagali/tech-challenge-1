package com.oficina.mecanica.infrastructure.persistence.entity;

import com.oficina.mecanica.domain.saga.DirecaoPasso;
import com.oficina.mecanica.domain.saga.ResultadoPasso;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_passos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SagaPassoJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "saga_id", nullable = false, columnDefinition = "uuid")
    private UUID sagaId;

    @Column(nullable = false)
    private int sequencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DirecaoPasso direcao;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(name = "mensagem_id", columnDefinition = "uuid")
    private UUID mensagemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoPasso resultado;

    @Column(length = 500)
    private String detalhe;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;
}
