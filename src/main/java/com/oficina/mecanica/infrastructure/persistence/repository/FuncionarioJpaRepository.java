package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.infrastructure.persistence.entity.FuncionarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FuncionarioJpaRepository extends JpaRepository<FuncionarioJpaEntity, UUID> {
    Optional<FuncionarioJpaEntity> findByCpf(String cpf);
}
