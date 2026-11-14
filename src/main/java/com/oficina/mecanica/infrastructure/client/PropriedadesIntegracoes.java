package com.oficina.mecanica.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.integracoes")
public record PropriedadesIntegracoes(Destino billing, Destino execution, Duration timeoutConexao,
                                      Duration timeoutLeitura) {

    public record Destino(String url) {}
}
