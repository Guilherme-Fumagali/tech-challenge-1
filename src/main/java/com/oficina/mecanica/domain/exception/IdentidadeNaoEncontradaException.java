package com.oficina.mecanica.domain.exception;

public class IdentidadeNaoEncontradaException extends RecursoNaoEncontradoException {
    public IdentidadeNaoEncontradaException() {
        super("Identidade não encontrada.");
    }
}
