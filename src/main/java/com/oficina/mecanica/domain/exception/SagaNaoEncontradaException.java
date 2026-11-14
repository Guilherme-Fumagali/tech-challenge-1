package com.oficina.mecanica.domain.exception;

import java.util.UUID;

public class SagaNaoEncontradaException extends RecursoNaoEncontradoException {
    public SagaNaoEncontradaException(UUID osId) {
        super("Saga da ordem de serviço", osId);
    }
}
