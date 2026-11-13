# Mapa de filas e mensagens

Cada fila pertence ao serviço que a consome e tem DLQ (`-dlq`, 5 recebimentos).

```mermaid
---
config:
  layout: elk
  flowchart:
    padding: 10
---
flowchart TB
    OS["OS Service<br/>(orquestrador)"]
    EX["Execution Service"]
    BI["Billing Service"]
    QE[["oficina-&lt;env&gt;-<br/>execution-comandos"]]
    QB[["oficina-&lt;env&gt;-<br/>billing-comandos"]]
    QO[["oficina-&lt;env&gt;-<br/>os-eventos"]]

    OS -->|"IniciarDiagnostico<br/>ReabrirDiagnostico<br/>IniciarReparo<br/>CancelarExecucao"| QE --> EX
    OS -->|"GerarOrcamento<br/>CancelarOrcamento<br/>EstornarPagamento"| QB --> BI
    EX -->|"ExecucaoEnfileirada<br/>DiagnosticoIniciado<br/>DiagnosticoConcluido<br/>ReparoIniciado<br/>ExecucaoFinalizada<br/>ExecucaoFalhou<br/>ExecucaoCancelada<br/>ComandoRejeitado"| QO
    BI -->|"OrcamentoGerado<br/>OrcamentoAprovado<br/>OrcamentoReprovado<br/>OrcamentoExpirado<br/>OrcamentoCancelado<br/>PagamentoAprovado<br/>PagamentoRecusado<br/>PagamentoEstornado<br/>ComandoRejeitado"| QO
    QO --> OS
```

Envelope comum: `mensagemId`, `tipo`, `versao`, `sagaId`, `osId`, `causadaPor`, `origem`, `ocorridoEm`, `dados`.
