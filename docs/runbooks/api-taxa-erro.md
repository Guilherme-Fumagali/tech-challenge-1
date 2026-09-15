# Runbook — Taxa de erro 5xx acima de 2%

**Alerta:** `AWS-OficinaAPI-<Amb>-API-TaxaErro5xx-Critical`
**Dispara quando:** mais de 2% das transações com erro por 5 minutos.

## O que significa

Erro de servidor. Diferente de 4xx, que é o cliente errando — 401 de token expirado, 422
de transição inválida — e que está fora deste alerta por configuração
(`ignore_status_codes: 401,403,404,422` no agente).

## Impacto

**Requisição falhando.** Toda resposta com erro de servidor conta como frustrada no
Apdex, independentemente da velocidade.

## Diagnóstico

1. **Qual erro e onde:**

```sql
SELECT count(*) FROM TransactionError
WHERE appName LIKE 'oficina-api%' FACET error.class, transactionName SINCE 30 minutes ago
```

2. **Errors Inbox** do New Relic agrupa por assinatura e mostra se o tipo é **novo** —
   erro novo logo após deploy é a pista mais forte.

3. **Log com stack trace**, correlacionado por `trace.id`:

```sql
SELECT message, `trace.id` FROM Log
WHERE `log.level` = 'ERROR' SINCE 30 minutes ago LIMIT 50
```

4. **Estado dos pods:**

```bash
kubectl get pods -n oficina -o wide
kubectl describe deployment/oficina-api -n oficina
kubectl get events -n oficina --sort-by=.lastTimestamp | tail -20
```

## Causas mais prováveis

| Sintoma | Causa provável | Ação |
|---|---|---|
| Erro novo logo após deploy | Regressão | **Rollback imediato** |
| `CannotGetJdbcConnectionException` | Pool esgotado ou RDS indisponível | Ver conexões no RDS; a Lambda executa no máximo 10 instâncias simultâneas |
| `OutOfMemoryError` | Heap insuficiente | Ver [k8s-memoria-pod.md](./k8s-memoria-pod.md) |
| Pods em `CrashLoopBackOff` | Falha de startup — migration, secret ausente | `kubectl logs --previous` |
| Erro só em `/api/ordens/*/aprovar-externo` | Integração externa | Ver [integracao-falhas.md](./integracao-falhas.md) |

## Mitigação

**Primeira ação em erro novo após deploy é rollback**, não investigação:

```bash
kubectl rollout undo deployment/oficina-api -n oficina
kubectl rollout status deployment/oficina-api -n oficina --timeout=180s
```

Investigar depois, com o serviço estável.

Se for o banco:

```bash
aws rds describe-db-instances --db-instance-identifier oficina-api-db-prod \
  --query 'DBInstances[0].DBInstanceStatus'
```

## Quando escalar

Taxa acima de 10%, ou erro que persiste após rollback — indica causa externa à aplicação
(banco, rede, NAT).
