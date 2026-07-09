package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.domain.valueobject.StatusOS;
import com.oficina.mecanica.infrastructure.persistence.entity.OrdemServicoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoJpaEntity, UUID> {

    List<OrdemServicoJpaEntity> findByStatus(StatusOS status);

    List<OrdemServicoJpaEntity> findByVeiculoId(UUID veiculoId);

    @Query("""
        SELECT os FROM OrdemServicoJpaEntity os
        WHERE os.status IN ('FINALIZADA', 'ENTREGUE')
          AND os.dataConclusao BETWEEN :inicio AND :fim
        """)
    List<OrdemServicoJpaEntity> findFinalizadasNoPeriodo(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim
    );

    @Query("""
        SELECT os FROM OrdemServicoJpaEntity os
        WHERE os.excluidaLogicamente = false
        ORDER BY CASE os.status
            WHEN 'EM_EXECUCAO' THEN 0
            WHEN 'AGUARDANDO_APROVACAO' THEN 1
            WHEN 'EM_DIAGNOSTICO' THEN 2
            WHEN 'RECEBIDA' THEN 3
            ELSE 4
        END, os.dataAbertura ASC
        """)
    List<OrdemServicoJpaEntity> findAtivasOrdenadasPorPrioridade();
}
