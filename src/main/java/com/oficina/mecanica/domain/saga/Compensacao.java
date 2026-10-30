package com.oficina.mecanica.domain.saga;

public enum Compensacao {
    CANCELAR_EXECUCAO(TipoComando.CANCELAR_EXECUCAO, TipoEvento.EXECUCAO_CANCELADA),
    CANCELAR_ORCAMENTO(TipoComando.CANCELAR_ORCAMENTO, TipoEvento.ORCAMENTO_CANCELADO),
    ESTORNAR_PAGAMENTO(TipoComando.ESTORNAR_PAGAMENTO, TipoEvento.PAGAMENTO_ESTORNADO),
    ESTORNAR_ESTOQUE(null, null);

    private final TipoComando comando;
    private final TipoEvento respostaQueLibera;

    Compensacao(TipoComando comando, TipoEvento respostaQueLibera) {
        this.comando = comando;
        this.respostaQueLibera = respostaQueLibera;
    }

    public TipoComando comando() {
        return comando;
    }

    public TipoEvento respostaQueLibera() {
        return respostaQueLibera;
    }

    public boolean local() {
        return comando == null;
    }
}
