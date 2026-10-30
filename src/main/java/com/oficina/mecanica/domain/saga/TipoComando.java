package com.oficina.mecanica.domain.saga;

public enum TipoComando {
    INICIAR_DIAGNOSTICO("IniciarDiagnostico", Participante.EXECUTION, true),
    REABRIR_DIAGNOSTICO("ReabrirDiagnostico", Participante.EXECUTION, false),
    INICIAR_REPARO("IniciarReparo", Participante.EXECUTION, true),
    CANCELAR_EXECUCAO("CancelarExecucao", Participante.EXECUTION, true),
    GERAR_ORCAMENTO("GerarOrcamento", Participante.BILLING, true),
    CANCELAR_ORCAMENTO("CancelarOrcamento", Participante.BILLING, true),
    ESTORNAR_PAGAMENTO("EstornarPagamento", Participante.BILLING, true);

    private final String nome;
    private final Participante destino;
    private final boolean exigeResposta;

    TipoComando(String nome, Participante destino, boolean exigeResposta) {
        this.nome = nome;
        this.destino = destino;
        this.exigeResposta = exigeResposta;
    }

    public String nome() {
        return nome;
    }

    public Participante destino() {
        return destino;
    }

    public boolean exigeResposta() {
        return exigeResposta;
    }

    public static TipoComando porNome(String nome) {
        for (var tipo : values()) {
            if (tipo.nome.equals(nome)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Comando desconhecido: " + nome);
    }
}
