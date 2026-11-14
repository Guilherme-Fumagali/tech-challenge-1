# Máquina de estados da saga

Estado persistido em `sagas_ordem_servico`. Estados com prazo de resposta têm reenvio automático do último comando (até 3 vezes) antes de irem para `FALHA`.

```mermaid
---
config:
  layout: elk
---
stateDiagram-v2
    [*] --> AGUARDANDO_FILA_DIAGNOSTICO: OS aberta / IniciarDiagnostico
    AGUARDANDO_FILA_DIAGNOSTICO --> AGUARDANDO_DIAGNOSTICO: ExecucaoEnfileirada
    AGUARDANDO_FILA_DIAGNOSTICO --> EM_DIAGNOSTICO: DiagnosticoIniciado
    AGUARDANDO_DIAGNOSTICO --> EM_DIAGNOSTICO: DiagnosticoIniciado
    EM_DIAGNOSTICO --> EM_DIAGNOSTICO: DiagnosticoConcluido sem itens / ReabrirDiagnostico
    EM_DIAGNOSTICO --> AGUARDANDO_ORCAMENTO: DiagnosticoConcluido / GerarOrcamento
    AGUARDANDO_ORCAMENTO --> AGUARDANDO_APROVACAO: OrcamentoGerado
    AGUARDANDO_ORCAMENTO --> COMPENSANDO: ComandoRejeitado
    AGUARDANDO_APROVACAO --> AGUARDANDO_PAGAMENTO: OrcamentoAprovado
    AGUARDANDO_APROVACAO --> COMPENSANDO: OrcamentoReprovado / OrcamentoExpirado
    AGUARDANDO_PAGAMENTO --> AGUARDANDO_FILA_REPARO: PagamentoAprovado / IniciarReparo
    AGUARDANDO_PAGAMENTO --> COMPENSANDO: PagamentoRecusado
    AGUARDANDO_FILA_REPARO --> EM_EXECUCAO: ExecucaoEnfileirada
    AGUARDANDO_FILA_REPARO --> COMPENSANDO: ExecucaoFalhou
    EM_EXECUCAO --> CONCLUIDA: ExecucaoFinalizada
    EM_EXECUCAO --> COMPENSANDO: ExecucaoFalhou
    COMPENSANDO --> COMPENSANDO: resposta da compensação / próxima compensação
    COMPENSANDO --> COMPENSADA: estorno de estoque local
    COMPENSANDO --> FALHA: compensação rejeitada
    CONCLUIDA --> [*]
    COMPENSADA --> [*]
```

| Motivo | Compensações, em ordem |
|---|---|
| Reprovação ou expiração do orçamento | cancelar execução → estornar estoque |
| Comando rejeitado | cancelar execução → estornar estoque |
| Pagamento recusado | cancelar orçamento → cancelar execução → estornar estoque |
| Falha na execução | estornar pagamento → cancelar orçamento → estornar estoque |
