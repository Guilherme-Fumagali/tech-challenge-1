package com.oficina.mecanica.infrastructure.security;

import com.oficina.mecanica.infrastructure.security.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SEGREDO = "chave-de-teste-super-longa-para-hmac-sha-256-minimo-32-chars";
    private static final String OUTRO_SEGREDO = "outra-chave-completamente-diferente-com-32-chars-no-minimo!!";

    private final JwtService service = new JwtService(SEGREDO);

    @Test
    void deveAceitarTokenEmitidoPelaLambdaDeAutenticacao() {
        var clienteId = UUID.randomUUID().toString();
        var token = tokenValido(SEGREDO, "oficina-auth", clienteId, 900);

        assertThat(service.isTokenValido(token)).isTrue();
        assertThat(service.extrairClienteId(token)).isEqualTo(clienteId);
        assertThat(service.extrairRole(token)).isEqualTo("CLIENTE");
    }

    @Test
    void deveAceitarTokenDeFuncionario() {
        var agora = Instant.now();
        var token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .issuer("oficina-auth")
            .claim("role", "FUNCIONARIO")
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThat(service.isTokenValido(token)).isTrue();
        assertThat(service.extrairRole(token)).isEqualTo("FUNCIONARIO");
    }

    @Test
    void deveRecusarTokenAssinadoComOutraChave() {
        var token = tokenValido(OUTRO_SEGREDO, "oficina-auth", UUID.randomUUID().toString(), 900);
        assertThat(service.isTokenValido(token)).isFalse();
    }

    @Test
    void deveRecusarTokenDeOutroEmissor() {
        var token = tokenValido(SEGREDO, "emissor-desconhecido", UUID.randomUUID().toString(), 900);
        assertThat(service.isTokenValido(token)).isFalse();
    }

    @Test
    void deveRecusarTokenExpirado() {
        var token = tokenValido(SEGREDO, "oficina-auth", UUID.randomUUID().toString(), -60);
        assertThat(service.isTokenValido(token)).isFalse();
    }

    @Test
    void deveRecusarRoleDesconhecida() {
        // A role vira authority no SecurityContext. Claim forjada com valor arbitrário
        // não pode virar ROLE_<lixo> silencioso.
        var agora = Instant.now();
        var token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .issuer("oficina-auth")
            .claim("role", "ADMIN")
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThat(service.isTokenValido(token)).isFalse();
    }

    @Test
    void deveRecusarTokenMalformado() {
        assertThat(service.isTokenValido("nao-e-um-jwt")).isFalse();
    }

    @Test
    void deveAssumirClienteQuandoRoleAusente() {
        var agora = Instant.now();
        var token = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .issuer("oficina-auth")
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
            .compact();

        assertThat(service.extrairRole(token)).isEqualTo("CLIENTE");
    }

    private String tokenValido(String segredo, String issuer, String sub, long segundos) {
        var agora = Instant.now();
        return Jwts.builder()
            .subject(sub)
            .issuer(issuer)
            .claim("cpf", "52998224725")
            .claim("nome", "Cliente de Teste")
            .claim("role", "CLIENTE")
            .issuedAt(Date.from(agora.minusSeconds(1)))
            .expiration(Date.from(agora.plusSeconds(segundos)))
            .signWith(Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }
}
