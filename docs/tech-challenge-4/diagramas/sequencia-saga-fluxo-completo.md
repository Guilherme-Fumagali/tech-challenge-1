# Sequência — saga da OS, fluxo completo

Comandos do orquestrador seguem para a fila de cada participante; respostas e eventos voltam pela fila `os-eventos`. Toda mensagem sai pelo outbox do serviço emissor.

```mermaid
---
config:
  sequence:
    wrap: false
    diagramMarginX: 12
    diagramMarginY: 10
    width: 90
    actorMargin: 30
    messageMargin: 34
    boxMargin: 8
    noteMargin: 12
    messageFontSize: 15
    noteFontSize: 14
    actorFontSize: 15
  themeVariables:
    noteBkgColor: "#f4f4f4"
    noteTextColor: "#222222"
    noteBorderColor: "#9a9a9a"
---
sequenceDiagram
    autonumber
    actor A as Atendente
    actor M as Mecânico
    actor C as Cliente
    participant OS as OS Service
    participant Q as Amazon SQS
    participant EX as Execution<br/>Service
    participant BI as Billing<br/>Service
    participant MP as Mercado<br/>Pago

    A->>OS: POST /api/ordens
    Note over OS: OS RECEBIDA<br/>+ saga + outbox<br/>(mesma transação)
    OS-)Q: IniciarDiagnostico<br/>→ execution-comandos
    Q-)EX: IniciarDiagnostico
    EX-)Q: ExecucaoEnfileirada<br/>→ os-eventos
    M->>EX: POST /api/execucoes/{osId}/diagnostico/inicio
    EX-)Q: DiagnosticoIniciado
    Q-)OS: DiagnosticoIniciado
    Note over OS: OS EM_DIAGNOSTICO
    M->>OS: POST /api/ordens/{id}/<br/>servicos e /pecas<br/>(reserva de estoque)
    M->>EX: POST …/diagnostico/conclusao
    EX-)Q: DiagnosticoConcluido
    Q-)OS: DiagnosticoConcluido
    OS-)Q: GerarOrcamento<br/>(snapshot dos itens)<br/>→ billing-comandos
    Q-)BI: GerarOrcamento
    BI->>C: e-mail com o orçamento e os links de decisão
    BI-)Q: OrcamentoGerado
    Q-)OS: OrcamentoGerado
    Note over OS: OS<br/>AGUARDANDO_APROVACAO
    C->>BI: POST /api/orcamentos/{id}/aprovacao
    BI->>MP: POST /checkout/<br/>preferences
    MP-->>BI: link de pagamento
    BI-)Q: OrcamentoAprovado
    Q-)OS: OrcamentoAprovado
    Note over OS: OS<br/>AGUARDANDO_PAGAMENTO
    C->>MP: paga com cartão
    MP->>BI: webhook<br/>(x-signature)
    BI->>MP: GET /v1/payments/{id}
    BI-)Q: PagamentoAprovado
    Q-)OS: PagamentoAprovado
    Note over OS: OS EM_EXECUCAO
    OS-)Q: IniciarReparo<br/>→ execution-comandos
    Q-)EX: IniciarReparo
    M->>EX: POST …/reparo/inicio e …/reparo/conclusao
    EX-)Q: ExecucaoFinalizada
    Q-)OS: ExecucaoFinalizada
    Note over OS: OS FINALIZADA<br/>saga CONCLUIDA
    A->>OS: POST /api/ordens/{id}/entregar
    Note over OS: OS ENTREGUE
```
