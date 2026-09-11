# Runbook — Falhas de integração

**Alerta:** `AWS-OficinaAPI-<Amb>-Integracao-Falhas-Warning`
**Dispara quando:** mais de 10 falhas em 15 minutos.

## O que significa

A métrica `oficina.integracao.falhas` é segmentada por `integracao` e `motivo`. Duas
integrações são instrumentadas:

| `integracao` | O que é |
|---|---|
| `email` | Envio do orçamento por SMTP, com os links de aprovação |
| `aprovacao_externa` | Recebimento da decisão do cliente pelo link do e-mail |

## Impacto

**O orçamento não chega ao cliente, ou a resposta dele não chega ao sistema.** A OS fica
parada em `AGUARDANDO_APROVACAO` sem que ninguém perceba pelo fluxo normal.

## Diagnóstico

1. **Qual integração e qual motivo:**

```sql
SELECT sum(oficina.integracao.falhas) FROM Metric
FACET integracao, motivo TIMESERIES AUTO SINCE 1 hour ago
```

2. **Se for `email`:**

```bash
kubectl logs -n oficina -l app=oficina-api --tail=200 | grep -i 'mail\|smtp'
kubectl get pods -n oficina -l app=oficina-mailhog
```

Conferir `SMTP_HOST` e `SMTP_PORT` no ConfigMap — em produção real, apontam para SMTP
externo, e aí a saída depende da **NAT instance**.

3. **Se for `aprovacao_externa`:** o motivo mais comum é token expirado ou já usado. Não é
   falha de sistema — é o cliente clicando num link antigo.

```sql
SELECT count(*) FROM Log
WHERE message LIKE '%Token de aprovação%' FACET message SINCE 1 hour ago
```

4. **Quantas OS estão paradas esperando:**

```sql
SELECT id, data_ultima_transicao FROM ordens_servico
WHERE status = 'AGUARDANDO_APROVACAO'
  AND data_ultima_transicao < NOW() - INTERVAL '48 hours';
```

## Causas mais prováveis

| `integracao` / `motivo` | Causa provável | Ação |
|---|---|---|
| `email` / `montagem_mensagem` | Cliente sem e-mail cadastrado | Corrigir o cadastro; validar o campo na entrada |
| `email`, todas falhando | SMTP fora, ou NAT sem saída | Ver [DT-04](../tech-challenge-3/debitos-tecnicos.md) |
| `aprovacao_externa` / token expirado | Cliente demorou mais que a validade (168 h padrão) | Reenviar o orçamento |
| Pico súbito em `aprovacao_externa` | Possível varredura de tokens | Verificar origem no access log do gateway |

## Mitigação

- **SMTP fora:** as ordens não se perdem — ficam em `AGUARDANDO_APROVACAO`. Reenviar o
  orçamento após restabelecer.
- **Token expirado legítimo:** gerar novo orçamento, o que emite token novo.
- **NAT sem saída:** conferir a instância.
  ```bash
  aws ec2 describe-instances --filters "Name=tag:Name,Values=oficina-api-nat" \
    --query 'Reservations[0].Instances[0].[State.Name,InstanceId]' --output text
  ```

## Quando escalar

Se OS ficarem paradas por mais de 48 h em `AGUARDANDO_APROVACAO` em volume, ou se o pico
de `aprovacao_externa` vier de um único IP.
