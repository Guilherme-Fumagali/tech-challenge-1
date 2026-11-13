package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.infrastructure.persistence.entity.HistoricoStatusOSJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistoricoStatusOSJpaRepository extends JpaRepository<HistoricoStatusOSJpaEntity, UUID> {

    List<HistoricoStatusOSJpaEntity> findByOsIdOrderByOcorridoEmAsc(UUID osId);
}
