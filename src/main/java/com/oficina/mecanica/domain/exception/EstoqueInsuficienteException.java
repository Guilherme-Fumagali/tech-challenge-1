package com.oficina.mecanica.domain.exception;

public class EstoqueInsuficienteException extends DomainException {
    public EstoqueInsuficienteException(String nomePeca, int solicitado, int disponivel) {
        super("Estoque insuficiente para '%s': solicitado=%d, disponível=%d"
            .formatted(nomePeca, solicitado, disponivel));
    }
}
