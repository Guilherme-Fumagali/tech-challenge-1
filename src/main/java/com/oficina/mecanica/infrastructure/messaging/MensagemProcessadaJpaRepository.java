package com.oficina.mecanica.infrastructure.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MensagemProcessadaJpaRepository extends JpaRepository<MensagemProcessadaJpaEntity, UUID> {

    @Modifying
    @Query(value = """
        INSERT INTO mensagens_processadas (mensagem_id, tipo, processada_em)
        VALUES (:mensagemId, :tipo, :processadaEm)
        ON CONFLICT (mensagem_id) DO NOTHING
        """, nativeQuery = true)
    int registrar(@Param("mensagemId") UUID mensagemId, @Param("tipo") String tipo,
                  @Param("processadaEm") LocalDateTime processadaEm);
}
