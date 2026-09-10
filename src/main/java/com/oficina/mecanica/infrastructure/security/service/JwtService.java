package com.oficina.mecanica.infrastructure.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private static final String ISSUER_ESPERADO = "oficina-auth";

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isTokenValido(String token) {
        try {
            var claims = parsearClaims(token);
            return ISSUER_ESPERADO.equals(claims.getIssuer());
        } catch (Exception e) {
            return false;
        }
    }

    public String extrairClienteId(String token) {
        return parsearClaims(token).getSubject();
    }

    public String extrairRole(String token) {
        var role = parsearClaims(token).get("role", String.class);
        return role == null ? "CLIENTE" : role;
    }

    private Claims parsearClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
