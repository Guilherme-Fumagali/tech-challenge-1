package com.oficina.mecanica.infrastructure.persistence.repository;

import com.oficina.mecanica.infrastructure.persistence.entity.PecaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PecaJpaRepository extends JpaRepository<PecaJpaEntity, UUID> {

    @Query("SELECT p FROM PecaJpaEntity p WHERE p.quantidadeEstoque < p.estoqueMinimo")
    List<PecaJpaEntity> findAbaixoDoEstoqueMinimo();
}
