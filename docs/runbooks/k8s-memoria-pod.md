# Runbook — Memória de pod acima de 85%

**Alerta:** `AWS-OficinaAPI-<Amb>-K8s-MemoriaPod-Warning`
**Dispara quando:** `memoryWorkingSetUtilization` acima de 85% por 10 minutos.

## O que significa

O container está próximo do `limits.memory` de 1 GiB. Ultrapassar significa **OOMKill**:
o kubelet mata o container e o Kubernetes reinicia, derrubando as requisições em voo.

## Contexto importante antes de agir

A JVM roda com `-XX:MaxRAMPercentage=70.0`, então **ela ocupa memória por design** — o heap
cresce até o limite antes de coletar. Memória alta e estável não é vazamento. O sinal
preocupante é memória alta **e crescente** entre coletas.

O `-javaagent` do New Relic também consome heap e adiciona ~1 s ao startup, motivo do
`failureThreshold: 40` no `startupProbe`.

## Diagnóstico

1. **Tendência, não instantâneo:**

```sql
SELECT average(memoryWorkingSetBytes)/1e6 FROM K8sContainerSample
WHERE containerName = 'oficina-api' FACET podName TIMESERIES AUTO SINCE 3 hours ago
```

Curva serrilhada = coleta funcionando. Curva subindo em escada = vazamento.

2. **Histórico de OOMKill:**

```bash
kubectl get pods -n oficina -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.status.containerStatuses[0].lastState.terminated.reason}{"\n"}{end}'
kubectl top pods -n oficina
```

3. **Correlacionar com carga:**

```sql
SELECT rate(count(*), 1 minute) FROM Transaction
WHERE appName LIKE 'oficina-api%' TIMESERIES SINCE 3 hours ago
```

## Causas mais prováveis

| Padrão | Causa provável | Ação |
|---|---|---|
| Alta e estável, sem restart | Comportamento normal da JVM | Ajustar o limiar, não a aplicação |
| Crescente entre coletas | Vazamento de memória | Investigar; considerar heap dump |
| Pico junto com pico de tráfego | Carga acima da capacidade | HPA deve escalar; conferir |
| Alta logo no startup, depois normal | Agente do New Relic + Flyway | Esperado |

## Mitigação

- **HPA travado no teto:** conferir se há nó disponível.
  ```bash
  kubectl get hpa -n oficina
  kubectl get nodes
  kubectl describe pod -n oficina -l app=oficina-api | grep -A5 Events
  ```
  Réplica em `Pending` por falta de nó significa que `maxReplicas` e `max_size` do node
  group precisam subir **juntos**.

- **Ajuste de heap:** reduzir `MaxRAMPercentage` para 60 dá folga ao overhead nativo sem
  mexer no limite do container.

- **OOMKill recorrente:** subir `limits.memory` só depois de descartar vazamento —
  aumentar o limite adia o sintoma sem tratar a causa.

## Quando escalar

OOMKill mais de duas vezes na mesma hora, ou memória crescendo continuamente por mais de
6 horas sem correlação com tráfego.
