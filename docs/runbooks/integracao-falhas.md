# Procedimento — Fontes indisponíveis no acompanhamento

**Sinal:** métrica `oficina.integracao.falhas` do OS Service, atributos `integracao`
(`billing` ou `execution`) e `motivo` (classe da exceção). Na Fase 4 este sinal aparece no
dashboard técnico e não tem condição de alerta própria; a condição da Fase 3 sobre o envio de
e-mail deixou de existir porque o e-mail saiu do OS Service.

## O que significa

O `GET /api/ordens/{id}/acompanhamento` compõe a resposta com chamadas ao Billing
(`GET /api/orcamentos?osId=`) e ao Execution (`GET /api/execucoes/{osId}`). Cada chamada tem
limite de 2 s, circuit breaker (janela de 10 chamadas, 50% de falhas, 30 s aberto) e bulkhead
de 10 chamadas simultâneas. Quando uma fonte falha, a resposta sai com o campo nulo e o nome
da fonte em `fontesIndisponiveis`; o contador registra o motivo.

| `motivo` | Origem |
|---|---|
| `TimeoutException` | Participante acima de 2 s |
| `CallNotPermittedException` | Circuito aberto após falhas seguidas |
| `BulkheadFullException` | Mais de 10 consultas simultâneas ao mesmo participante |
| `HttpServerErrorException...` | Participante respondeu 5xx |

## Impacto

O acompanhamento continua respondendo com os dados locais da OS e do histórico. A saga não
depende dessas chamadas.

## Diagnóstico

```sql
SELECT sum(oficina.integracao.falhas) FROM Metric
WHERE env = '<env>' FACET integracao, motivo TIMESERIES AUTO SINCE 1 hour ago
```

```bash
kubectl get pods -n oficina -l app.kubernetes.io/name=billing-service
kubectl get pods -n oficina -l app.kubernetes.io/name=execution-service
kubectl exec -n oficina deploy/os-service -- wget -qO- http://billing-service/actuator/health
```

## Mitigação

Recuperar o participante. O circuito fecha sozinho: depois de 30 s aberto, deixa passar duas
chamadas de teste e volta ao estado fechado se elas tiverem sucesso.
