package com.oficina.mecanica.infrastructure.security.service;

import com.oficina.mecanica.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.oficina.mecanica.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenJpaRepository repository;
    private final long refreshExpirationMs;

    public RefreshTokenService(
            RefreshTokenJpaRepository repository,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.repository = repository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public String gerar(String username) {
        var token = UUID.randomUUID().toString();
        var expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusNanos(refreshExpirationMs * 1_000_000L);

        var entity = new RefreshTokenJpaEntity(
            UUID.randomUUID(), token, username, expiresAt, LocalDateTime.now(ZoneOffset.UTC), false);
        repository.save(entity);
        return token;
    }

    @Transactional
    public String rotacionar(String tokenAtual) {
        var entity = repository.findByToken(tokenAtual)
            .orElseThrow(() -> new BadCredentialsException("Refresh token inválido."));

        if (entity.isRevogado()) {
            repository.revogarTodosPorUsername(entity.getUsername());
            throw new BadCredentialsException(
                "Refresh token já utilizado. Todos os tokens deste usuário foram revogados.");
        }

        if (entity.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BadCredentialsException("Refresh token expirado. Faça login novamente.");
        }

        entity.setRevogado(true);
        repository.save(entity);

        return gerar(entity.getUsername());
    }

    public String extrairUsername(String token) {
        return repository.findByToken(token)
            .orElseThrow(() -> new BadCredentialsException("Refresh token inválido."))
            .getUsername();
    }

    @Transactional
    public void revogar(String token) {
        repository.findByToken(token).ifPresent(e -> {
            e.setRevogado(true);
            repository.save(e);
        });
    }
}
