package com.oficina.mecanica.domain.saga;

import java.util.Optional;

public enum TipoEvento {
    EXECUCAO_ENFILEIRADA("ExecucaoEnfileirada"),
    DIAGNOSTICO_INICIADO("DiagnosticoIniciado"),
    DIAGNOSTICO_CONCLUIDO("DiagnosticoConcluido"),
    REPARO_INICIADO("ReparoIniciado"),
    EXECUCAO_FINALIZADA("ExecucaoFinalizada"),
    EXECUCAO_FALHOU("ExecucaoFalhou"),
    EXECUCAO_CANCELADA("ExecucaoCancelada"),
    ORCAMENTO_GERADO("OrcamentoGerado"),
    ORCAMENTO_APROVADO("OrcamentoAprovado"),
    ORCAMENTO_REPROVADO("OrcamentoReprovado"),
    ORCAMENTO_EXPIRADO("OrcamentoExpirado"),
    ORCAMENTO_CANCELADO("OrcamentoCancelado"),
    PAGAMENTO_APROVADO("PagamentoAprovado"),
    PAGAMENTO_RECUSADO("PagamentoRecusado"),
    PAGAMENTO_ESTORNADO("PagamentoEstornado"),
    COMANDO_REJEITADO("ComandoRejeitado");

    private final String nome;

    TipoEvento(String nome) {
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }

    public static Optional<TipoEvento> porNome(String nome) {
        for (var tipo : values()) {
            if (tipo.nome.equals(nome)) {
                return Optional.of(tipo);
            }
        }
        return Optional.empty();
    }
}
