# Runbook — Saga em FALHA e pico de compensações

**Alerta:** `AWS-Oficina-<Amb>-Saga-Falha-Critical`
**Dispara quando:** `sum(oficina.saga.falhas) > 0` (atributo `estado` = estado em que a saga parou).

## O que significa

O orquestrador do OS Service não consegue continuar a saga sem intervenção. Há três caminhos
para o estado `FALHA`:

| Origem | Registro no log da saga |
|---|---|
| Prazo de resposta esgotado depois de `app.saga.max-reenvios` (padrão 3) reenvios | passo `LOCAL` `PrazoEsgotado` |
| `ComandoRejeitado` em resposta a um comando de compensação | passo `RECEBIDO` `ComandoRejeitado` com o motivo |
| `ComandoRejeitado` para `IniciarDiagnostico` ou `IniciarReparo` | passo `RECEBIDO` `ComandoRejeitado` com o motivo |

Em `FALHA` a saga ignora novos eventos e o job de prazos não a reenvia mais. O status da OS
fica como estava.

## Impacto

A OS afetada não avança. Se a falha ocorreu depois do pagamento (estado `AGUARDANDO_FILA_REPARO`
ou `COMPENSANDO` por `FALHA_EXECUCAO`), há valor recebido que ainda não foi estornado.

## Diagnóstico

1. **Quais sagas e a partir de que estado:**

```sql
SELECT sum(oficina.saga.falhas) FROM Metric WHERE env = '<env>' FACET estado SINCE 1 day ago
```

```sql
SELECT message, `saga.id` FROM Log
WHERE message LIKE 'Saga % em FALHA%' SINCE 1 day ago
```

2. **Passos da saga**, com horário, direção, tipo e `mensagemId`:

```bash
curl -s "$API_URL/api/ordens/<osId>/saga" -H "Authorization: Bearer <token de funcionário>"
```

3. **Participante envolvido:** o último comando (`ultimoComando`) indica a fila. Conferir se o
   serviço está no ar e se a fila tem mensagens paradas:

```bash
kubectl get pods -n oficina -l app.kubernetes.io/name=billing-service
kubectl get pods -n oficina -l app.kubernetes.io/name=execution-service
aws sqs get-queue-attributes --queue-url "$(aws sqs get-queue-url --queue-name oficina-<env>-billing-comandos \
  --query QueueUrl --output text)" --attribute-names ApproximateNumberOfMessages
```

## Mitigação

- **Participante estava fora e voltou:** reativar a saga a partir do banco do OS Service,
  restaurando o estado anterior e vencendo o prazo para que o job reenvie o último comando
  com novo `mensagemId`. Os comandos são idempotentes por `osId` no participante.

```sql
UPDATE sagas_ordem_servico
   SET estado = '<estado anterior, visto no passo>', tentativas_reenvio = 0,
       prazo_resposta = now() - interval '1 minute', atualizada_em = now()
 WHERE os_id = '<osId>' AND estado = 'FALHA';
```

- **Compensação rejeitada pelo participante:** resolver a pendência no participante (por
  exemplo, estorno manual no painel do Mercado Pago) e encerrar a OS pela mesma regra da
  compensação local, devolvendo as peças ao estoque e cancelando a OS em uma única transação.
- Registrar no chamado o `osId`, o `sagaId`, os passos consultados e a ação tomada.

## Quando escalar

Sempre que houver pagamento aprovado envolvido, ou mais de uma saga em `FALHA` pelo mesmo
motivo em uma hora.

## Pico de compensações

**Alerta:** `AWS-Oficina-<Amb>-Saga-Compensacoes-Warning`
**Dispara quando:** `sum(oficina.saga.compensadas)` acima de 5 em 15 minutos.

Mais ordens de serviço do que o normal foram canceladas pelas compensações. O sistema está
funcionando; o alerta existe porque um volume fora do padrão costuma ter causa comum. O
atributo `motivo` separa as causas:

```sql
SELECT sum(oficina.saga.compensadas) FROM Metric
WHERE env = '<env>' FACET motivo TIMESERIES AUTO SINCE 3 hours ago
```

| `motivo` | Causa comum | Ação |
|---|---|---|
| `PAGAMENTO_RECUSADO` | recusas em sequência no Mercado Pago | ver [integracao-falhas.md](./integracao-falhas.md) |
| `REJEICAO` | cadastro do cliente incompleto (sem e-mail) | corrigir o cadastro e abrir nova OS |
| `REPROVACAO` | orçamentos expirando em massa | revisar a validade do orçamento no Billing |
| `FALHA_EXECUCAO` | falta de peça ou erro no reparo | conferir os apontamentos no Execution |

Escalar quando o pico persistir por mais de uma hora com o mesmo `motivo`.
