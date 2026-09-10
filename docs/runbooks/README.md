# Runbooks

Um procedimento por alerta. A Aula 05 de Monitoramento Avançado é categórica: *"cada
alerta disparado deve ter uma ação clara associada"*, e a pergunta-teste é *"o que eu
espero que alguém faça com esta informação?"*.

**Alerta sem runbook não entra.** Cada alert condition criada em
`oficina-infra-k8s/newrelic.tf` referencia um arquivo desta pasta na sua mensagem de
notificação.

| Alerta | Runbook |
|---|---|
| `AWS-OficinaAPI-<Amb>-OS-FalhaTransicao-Critical` | [os-falha-transicao.md](./os-falha-transicao.md) |
| `AWS-OficinaAPI-<Amb>-API-LatenciaP95-Warning` | [api-latencia-alta.md](./api-latencia-alta.md) |
| `AWS-OficinaAPI-<Amb>-API-TaxaErro5xx-Critical` | [api-taxa-erro.md](./api-taxa-erro.md) |
| `AWS-OficinaAPI-<Amb>-K8s-MemoriaPod-Warning` | [k8s-memoria-pod.md](./k8s-memoria-pod.md) |
| `AWS-OficinaAPI-<Amb>-Integracao-Falhas-Warning` | [integracao-falhas.md](./integracao-falhas.md) |
| `AWS-OficinaAPI-<Amb>-Health-Uptime-Critical` | [health-uptime.md](./health-uptime.md) |

## Formato

Cada runbook responde, nesta ordem: **o que o alerta significa**, **impacto**,
**diagnóstico** (comandos e queries), **mitigação** e **quando escalar**.
