package com.oficina.mecanica.infrastructure.web.dto.response;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tipo,
    long expiresIn
) {
    public TokenResponse(String accessToken, String refreshToken, long expiresInMs) {
        this(accessToken, refreshToken, "Bearer", expiresInMs / 1000);
    }
}
