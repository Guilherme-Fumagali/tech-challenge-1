package com.oficina.mecanica.infrastructure.persistence.entity;

import com.oficina.mecanica.domain.saga.EstadoSaga;
import com.oficina.mecanica.domain.saga.MotivoCompensacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sagas_ordem_servico")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SagaOrdemServicoJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "os_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID osId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EstadoSaga estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_compensacao", length = 40)
    private MotivoCompensacao motivoCompensacao;

    @Column(name = "compensacoes_pendentes", nullable = false, length = 200)
    private String compensacoesPendentes;

    @Column(name = "ultimo_comando_tipo", length = 60)
    private String ultimoComandoTipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ultimo_comando_dados", columnDefinition = "jsonb")
    private String ultimoComandoDados;

    @Column(name = "prazo_resposta")
    private LocalDateTime prazoResposta;

    @Column(name = "tentativas_reenvio", nullable = false)
    private int tentativasReenvio;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    @Version
    @Column(nullable = false)
    private Long versao;
}
