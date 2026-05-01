package com.oficina.mecanica.domain.exception;

import java.util.UUID;

public class RecursoNaoEncontradoException extends DomainException {
    public RecursoNaoEncontradoException(String recurso, UUID id) {
        super("%s não encontrado: %s".formatted(recurso, id));
    }
    public RecursoNaoEncontradoException(String recurso, String identificador) {
        super("%s não encontrado: %s".formatted(recurso, identificador));
    }
}
