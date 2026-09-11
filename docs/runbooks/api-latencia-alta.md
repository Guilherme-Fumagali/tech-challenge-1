# Runbook — Latência p95 acima de 1 segundo

**Alerta:** `AWS-OficinaAPI-<Amb>-API-LatenciaP95-Warning`
**Dispara quando:** p95 acima de 1000 ms por 10 minutos.

## O que significa

95% das requisições estão levando mais de 1 segundo. O Apdex T está em 0,5 s, então
requisições acima de 2 s já contam como **frustradas**.

## Impacto

Degradação perceptível. Ainda não é indisponibilidade — por isso é Warning, não Critical.

## Diagnóstico

1. **Isolar o endpoint.** Latência média mente; olhar por transação:

```sql
SELECT percentile(duration, 95) * 1000 FROM Transaction
WHERE appName LIKE 'oficina-api%' FACET name SINCE 30 minutes ago LIMIT 10
```

2. **Separar gateway de aplicação.** O access log traz `integrationLatency` e
   `responseLatency` — a diferença entre os dois é o gateway e o authorizer:

```bash
aws logs tail /aws/apigateway/oficina-api-prod --since 30m --format short \
  | jq -r 'select(.responseLatency > 1000) | "\(.routeKey) resp=\(.responseLatency) integ=\(.integrationLatency)"'
```

3. **Banco.** O parameter group registra query acima de 1 s:

```sql
SELECT average(databaseDuration) * 1000 FROM Transaction
WHERE appName LIKE 'oficina-api%' FACET name SINCE 30 minutes ago
```

4. **Recursos.** Ver se coincide com CPU ou memória alta — ver
   [k8s-memoria-pod.md](./k8s-memoria-pod.md).

## Causas mais prováveis

| Onde o tempo está | Causa provável | Ação |
|---|---|---|
| `integrationLatency` baixa, `responseLatency` alta | Cold start do Lambda authorizer | Esperado após ociosidade; verificar taxa de cache hit |
| `databaseDuration` alta | Query sem índice ou tabela crescida | Conferir `EXPLAIN` da query; os índices de V1/V6 cobrem os casos conhecidos |
| CPU do pod no teto | Carga acima da capacidade | Ver se o HPA escalou; teto é 4 réplicas |
| Latência uniforme em todos os endpoints | Saturação de rede pela NAT instance | `t4g.nano` é burstable — ver DT-04 |

## Mitigação

- **HPA não escalou:** conferir `metrics-server` e o estado do HPA.
  ```bash
  kubectl get hpa -n oficina
  kubectl top pods -n oficina
  ```
- **Query lenta:** adicionar índice em migration, não em produção manualmente.
- **Carga legítima e sustentada:** aumentar `max_size` do node group e `maxReplicas`
  juntos — os dois precisam subir, senão a réplica extra fica `Pending`.

## Quando escalar

Se a p95 passar de 3 s, ou se a taxa de erro subir junto — aí vira o cenário de
[api-taxa-erro.md](./api-taxa-erro.md).
