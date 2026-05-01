package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.infrastructure.persistence.entity.ClienteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, UUID> {
    Optional<ClienteJpaEntity> findByCpfCnpj(String cpfCnpj);
    boolean existsByCpfCnpj(String cpfCnpj);
}
