package com.oficina.mecanica;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public final class TokensDeTeste {

    private TokensDeTeste() {}

    public static String assinar(String segredo, String sujeito, String papel) {
        var agora = Instant.now();
        return Jwts.builder()
            .subject(sujeito)
            .issuer("oficina-auth")
            .claim("role", papel)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }
}
