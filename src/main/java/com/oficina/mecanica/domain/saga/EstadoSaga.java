package com.oficina.mecanica.domain.saga;

public enum EstadoSaga {
    AGUARDANDO_FILA_DIAGNOSTICO(0),
    AGUARDANDO_DIAGNOSTICO(1),
    EM_DIAGNOSTICO(2),
    AGUARDANDO_ORCAMENTO(3),
    AGUARDANDO_APROVACAO(4),
    AGUARDANDO_PAGAMENTO(5),
    AGUARDANDO_FILA_REPARO(6),
    EM_EXECUCAO(7),
    COMPENSANDO(-1),
    CONCLUIDA(8),
    COMPENSADA(-1),
    FALHA(-1);

    private final int posicaoNoFluxo;

    EstadoSaga(int posicaoNoFluxo) {
        this.posicaoNoFluxo = posicaoNoFluxo;
    }

    int posicaoNoFluxo() {
        return posicaoNoFluxo;
    }

    public boolean encerrado() {
        return this == CONCLUIDA || this == COMPENSADA || this == FALHA;
    }
}
