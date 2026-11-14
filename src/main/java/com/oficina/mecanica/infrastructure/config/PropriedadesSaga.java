package com.oficina.mecanica.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.saga")
public record PropriedadesSaga(Duration prazoResposta, long verificacaoPrazos, int maxReenvios, int lote) {}
