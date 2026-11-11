package com.oficina.mecanica.application.usecase.saga;

public enum ResultadoConsumo {
    APLICADA,
    DUPLICADA,
    IGNORADA,
    REJEITADA;

    public String rotulo() {
        return name().toLowerCase();
    }
}
