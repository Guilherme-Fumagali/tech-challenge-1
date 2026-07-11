package com.oficina.mecanica.infrastructure.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms:900000}") long accessExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
    }

    public String gerarAccessToken(String username) {
        Instant agora = Instant.now();
        return Jwts.builder()
            .subject(username)
            .claim("typ", "access")
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusMillis(accessExpirationMs)))
            .signWith(key)
            .compact();
    }

    public String extrairUsername(String token) {
        return parsearClaims(token).getSubject();
    }

    public boolean isTokenValido(String token) {
        try {
            parsearClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
