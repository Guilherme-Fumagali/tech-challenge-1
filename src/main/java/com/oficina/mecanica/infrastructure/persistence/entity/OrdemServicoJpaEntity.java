package com.oficina.mecanica.infrastructure.persistence.entity;

import com.oficina.mecanica.domain.valueobject.StatusOS;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrdemServicoJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "cliente_id", nullable = false, columnDefinition = "uuid")
    private UUID clienteId;

    @Column(name = "veiculo_id", nullable = false, columnDefinition = "uuid")
    private UUID veiculoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_aprovacao")
    private LocalDateTime dataAprovacao;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    @Column(name = "excluida_logicamente", nullable = false)
    private boolean excluidaLogicamente;

    @Column(name = "data_exclusao_logica")
    private LocalDateTime dataExclusaoLogica;

    @Column(name = "token_aprovacao_externa", length = 64)
    private String tokenAprovacaoExterna;

    @Column(name = "token_expiracao")
    private LocalDateTime tokenExpiracao;

    @Column(name = "data_ultima_transicao", nullable = false)
    private LocalDateTime dataUltimaTransicao;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemServicoJpaEntity> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPecaJpaEntity> itensPeca = new ArrayList<>();
}
