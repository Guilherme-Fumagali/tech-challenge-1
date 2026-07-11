package com.oficina.mecanica.infrastructure.web.controller;

import com.oficina.mecanica.infrastructure.security.service.JwtService;
import com.oficina.mecanica.infrastructure.security.service.RefreshTokenService;
import com.oficina.mecanica.infrastructure.web.dto.request.LoginRequest;
import com.oficina.mecanica.infrastructure.web.dto.request.RefreshRequest;
import com.oficina.mecanica.infrastructure.web.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authManager,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar e obter access token + refresh token")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        var accessToken  = jwtService.gerarAccessToken(request.username());
        var refreshToken = refreshTokenService.gerar(request.username());

        return ResponseEntity.ok(new TokenResponse(
            accessToken, refreshToken, jwtService.getAccessExpirationMs()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token usando refresh token (rotação automática)")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var novoRefreshToken = refreshTokenService.rotacionar(request.refreshToken());
        var username         = refreshTokenService.extrairUsername(novoRefreshToken);
        var novoAccessToken  = jwtService.gerarAccessToken(username);

        return ResponseEntity.ok(new TokenResponse(
            novoAccessToken, novoRefreshToken, jwtService.getAccessExpirationMs()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão — revoga o refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revogar(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
