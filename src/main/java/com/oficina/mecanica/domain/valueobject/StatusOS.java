package com.oficina.mecanica.domain.valueobject;

import com.oficina.mecanica.domain.exception.TransicaoInvalidaException;

import java.util.Set;

public enum StatusOS {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    AGUARDANDO_PAGAMENTO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    public void validarTransicaoPara(StatusOS destino) {
        boolean permitida = switch (this) {
            case RECEBIDA             -> destino == EM_DIAGNOSTICO;
            case EM_DIAGNOSTICO       -> Set.of(AGUARDANDO_APROVACAO, CANCELADA).contains(destino);
            case AGUARDANDO_APROVACAO -> Set.of(AGUARDANDO_PAGAMENTO, CANCELADA).contains(destino);
            case AGUARDANDO_PAGAMENTO -> Set.of(EM_EXECUCAO, CANCELADA).contains(destino);
            case EM_EXECUCAO          -> Set.of(FINALIZADA, CANCELADA).contains(destino);
            case FINALIZADA           -> destino == ENTREGUE;
            case ENTREGUE, CANCELADA  -> false;
        };
        if (!permitida) {
            throw new TransicaoInvalidaException(
                "Transição inválida: %s → %s".formatted(this, destino));
        }
    }
}
