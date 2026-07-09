package com.oficina.mecanica.domain.valueobject;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenAprovacaoExterna {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TAMANHO_BYTES = 32;

    private TokenAprovacaoExterna() {}

    public static String gerar() {
        byte[] bytes = new byte[TAMANHO_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
