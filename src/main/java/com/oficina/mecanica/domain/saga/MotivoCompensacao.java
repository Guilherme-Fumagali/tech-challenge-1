package com.oficina.mecanica.domain.saga;

import java.util.List;

import static com.oficina.mecanica.domain.saga.Compensacao.CANCELAR_EXECUCAO;
import static com.oficina.mecanica.domain.saga.Compensacao.CANCELAR_ORCAMENTO;
import static com.oficina.mecanica.domain.saga.Compensacao.ESTORNAR_ESTOQUE;
import static com.oficina.mecanica.domain.saga.Compensacao.ESTORNAR_PAGAMENTO;

public enum MotivoCompensacao {
    REPROVACAO(List.of(CANCELAR_EXECUCAO, ESTORNAR_ESTOQUE)),
    REJEICAO(List.of(CANCELAR_EXECUCAO, ESTORNAR_ESTOQUE)),
    PAGAMENTO_RECUSADO(List.of(CANCELAR_ORCAMENTO, CANCELAR_EXECUCAO, ESTORNAR_ESTOQUE)),
    FALHA_EXECUCAO(List.of(ESTORNAR_PAGAMENTO, CANCELAR_ORCAMENTO, ESTORNAR_ESTOQUE));

    private final List<Compensacao> compensacoes;

    MotivoCompensacao(List<Compensacao> compensacoes) {
        this.compensacoes = compensacoes;
    }

    public List<Compensacao> compensacoes() {
        return compensacoes;
    }
}
