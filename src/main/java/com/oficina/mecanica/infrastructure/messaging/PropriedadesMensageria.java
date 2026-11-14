package com.oficina.mecanica.infrastructure.messaging;

import com.oficina.mecanica.domain.saga.Participante;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.mensageria")
public record PropriedadesMensageria(String origem, Filas filas, String dlq, Relay relay) {

    public static final String OS_EVENTOS = "os-eventos";
    public static final String BILLING_COMANDOS = "billing-comandos";
    public static final String EXECUTION_COMANDOS = "execution-comandos";

    public record Filas(String osEventos, String billingComandos, String executionComandos, String entrada) {}

    public record Relay(long intervalo, int lote) {}

    public String chaveDaFila(Participante participante) {
        return switch (participante) {
            case BILLING -> BILLING_COMANDOS;
            case EXECUTION -> EXECUTION_COMANDOS;
        };
    }

    public String nomeDaFila(String chave) {
        return switch (chave) {
            case OS_EVENTOS -> filas.osEventos();
            case BILLING_COMANDOS -> filas.billingComandos();
            case EXECUTION_COMANDOS -> filas.executionComandos();
            default -> throw new IllegalArgumentException("Fila desconhecida: " + chave);
        };
    }
}
