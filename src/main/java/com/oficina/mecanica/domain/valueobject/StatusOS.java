package com.oficina.mecanica.domain.valueobject;

import com.oficina.mecanica.domain.exception.TransicaoInvalidaException;

import java.util.Set;

public enum StatusOS {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    public void validarTransicaoPara(StatusOS destino) {
        boolean permitida = switch (this) {
            case RECEBIDA             -> destino == EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO       -> destino == AGUARDANDO_APROVACAO;
            case AGUARDANDO_APROVACAO -> Set.of(EM_EXECUCAO, CANCELADA).contains(destino);
            case EM_EXECUCAO          -> destino == FINALIZADA;
            case FINALIZADA           -> destino == ENTREGUE;
            case ENTREGUE, CANCELADA  -> false;
        };
        if (!permitida) {
            throw new TransicaoInvalidaException(
                "Transição inválida: %s → %s".formatted(this, destino));
        }
    }

    public int prioridadeListagem() {
        return switch (this) {
            case EM_EXECUCAO -> 0;
            case AGUARDANDO_APROVACAO -> 1;
            case EM_DIAGNOSTICO -> 2;
            case RECEBIDA -> 3;
            case FINALIZADA, ENTREGUE, CANCELADA -> 4;
        };
    }
}
