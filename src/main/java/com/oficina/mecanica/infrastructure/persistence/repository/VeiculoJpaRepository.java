package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.infrastructure.persistence.entity.VeiculoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoJpaRepository extends JpaRepository<VeiculoJpaEntity, UUID> {
    Optional<VeiculoJpaEntity> findByPlaca(String placa);
    boolean existsByPlaca(String placa);
    List<VeiculoJpaEntity> findByClienteId(UUID clienteId);
}
