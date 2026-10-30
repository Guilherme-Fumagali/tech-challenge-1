package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.infrastructure.persistence.entity.SagaPassoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SagaPassoJpaRepository extends JpaRepository<SagaPassoJpaEntity, UUID> {

    List<SagaPassoJpaEntity> findBySagaIdOrderBySequencia(UUID sagaId);

    @Query("SELECT COALESCE(MAX(p.sequencia), 0) FROM SagaPassoJpaEntity p WHERE p.sagaId = :sagaId")
    int ultimaSequencia(@Param("sagaId") UUID sagaId);
}
