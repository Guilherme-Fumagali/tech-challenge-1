package com.oficina.mecanica.domain.exception;

public class EventoForaDeOrdemException extends DomainException {
    public EventoForaDeOrdemException(String evento, String estado) {
        super("Evento %s recebido antes do esperado; a saga está em %s.".formatted(evento, estado));
    }
}
