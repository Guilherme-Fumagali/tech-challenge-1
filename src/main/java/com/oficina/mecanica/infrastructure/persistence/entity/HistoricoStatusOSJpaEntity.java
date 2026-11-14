package com.oficina.mecanica.infrastructure.persistence.entity;

import com.oficina.mecanica.domain.valueobject.StatusOS;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historico_status_os")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class HistoricoStatusOSJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "os_id", nullable = false, columnDefinition = "uuid")
    private UUID osId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_origem", length = 30)
    private StatusOS statusOrigem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_destino", nullable = false, length = 30)
    private StatusOS statusDestino;

    @Column(nullable = false, length = 60)
    private String evento;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;
}
