package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.infrastructure.persistence.entity.SagaOrdemServicoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaOrdemServicoJpaRepository extends JpaRepository<SagaOrdemServicoJpaEntity, UUID> {

    Optional<SagaOrdemServicoJpaEntity> findByOsId(UUID osId);

    @Query(value = """
        SELECT * FROM sagas_ordem_servico
        WHERE prazo_resposta < :agora
        ORDER BY prazo_resposta
        LIMIT :limite
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<SagaOrdemServicoJpaEntity> reivindicarComPrazoVencido(@Param("agora") LocalDateTime agora,
                                                               @Param("limite") int limite);
}
