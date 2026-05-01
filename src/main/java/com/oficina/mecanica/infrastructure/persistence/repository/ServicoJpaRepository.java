package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.infrastructure.persistence.entity.ServicoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServicoJpaRepository extends JpaRepository<ServicoJpaEntity, UUID> {
}
