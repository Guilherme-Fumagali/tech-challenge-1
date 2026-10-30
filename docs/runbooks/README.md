# Runbooks

Um procedimento por alerta. A Aula 05 de Monitoramento Avançado é categórica: *"cada
alerta disparado deve ter uma ação clara associada"*, e a pergunta-teste é *"o que eu
espero que alguém faça com esta informação?"*.

**Alerta sem runbook não entra.** Cada condição criada em
`oficina-infra-k8s/cluster/newrelic.tf` referencia um arquivo desta pasta no campo
`runbook_url` e na descrição. A política `AWS-Oficina-<Amb>` notifica o e-mail da equipe por
um workflow do New Relic.

Os nomes seguem o padrão `AWS-Oficina-<Amb>-<Recurso>-<Sintoma>-<Sev>`. As condições por
serviço usam `<Recurso>` igual a `OsService`, `BillingService` ou `ExecutionService`.

| Alerta | Runbook |
|---|---|
| `AWS-Oficina-<Amb>-<Serviço>-LatenciaP95-Warning` | [api-latencia-alta.md](./api-latencia-alta.md) |
| `AWS-Oficina-<Amb>-<Serviço>-TaxaErro5xx-Critical` | [api-taxa-erro.md](./api-taxa-erro.md) |
| `AWS-Oficina-<Amb>-<Serviço>-MemoriaPod-Warning` | [k8s-memoria-pod.md](./k8s-memoria-pod.md) |
| `AWS-Oficina-<Amb>-Mensageria-DLQ-Critical` | [mensageria-dlq.md](./mensageria-dlq.md) |
| `AWS-Oficina-<Amb>-Saga-Falha-Critical` | [saga-falha.md](./saga-falha.md) |
| `AWS-Oficina-<Amb>-Saga-Compensacoes-Warning` | [saga-compensacoes.md](./saga-compensacoes.md) |
| `AWS-Oficina-<Amb>-MercadoPago-Falhas-Warning` | [mercadopago-falhas.md](./mercadopago-falhas.md) |
| `AWS-Oficina-<Amb>-OS-FalhaTransicao-Critical` | [os-falha-transicao.md](./os-falha-transicao.md) |
| `AWS-Oficina-<Amb>-Health-Uptime-Critical` | [health-uptime.md](./health-uptime.md) |

Procedimento sem condição de alerta: [integracao-falhas.md](./integracao-falhas.md), para as
fontes indisponíveis no acompanhamento composto da OS.

## Formato

Cada runbook responde, nesta ordem: **o que o alerta significa**, **impacto**,
**diagnóstico** (comandos e queries), **mitigação** e **quando escalar**.
