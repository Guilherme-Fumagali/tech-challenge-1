# Runbook — Mensagem na DLQ

**Alerta:** `AWS-Oficina-<Amb>-Mensageria-DLQ-Critical`
**Dispara quando:** `max(oficina.mensageria.dlq.mensagens) > 0` em qualquer fila (atributo `fila`).

## O que significa

Uma mensagem foi recebida 5 vezes sem ser processada com sucesso e o SQS a moveu para a
fila `-dlq` (`maxReceiveCount = 5`, visibilidade de 30 s). Cada serviço publica a métrica da
própria DLQ: o OS Service mede `oficina-<env>-os-eventos-dlq`, o Billing mede
`oficina-<env>-billing-comandos-dlq` e o Execution mede `oficina-<env>-execution-comandos-dlq`.

Rejeição de negócio não chega aqui: o participante responde `ComandoRejeitado` e reconhece a
mensagem. Na DLQ ficam apenas falhas técnicas repetidas: mensagem fora do formato do
envelope, evento fora de ordem que nunca encontrou o estado esperado, saga ou OS
inexistente, banco indisponível por mais de cinco tentativas.

## Impacto

A saga da OS correspondente parou no estado em que estava. Não há perda de dado: a mensagem
fica retida por 14 dias na DLQ, e o estado da saga continua no banco do OS Service.

## Diagnóstico

1. **Qual fila e desde quando:**

```sql
SELECT max(oficina.mensageria.dlq.mensagens) FROM Metric
WHERE env = '<env>' FACET fila TIMESERIES AUTO SINCE 3 hours ago
```

2. **Qual tipo de mensagem falhou e quantas vezes:**

```sql
SELECT sum(oficina.mensageria.consumidas) FROM Metric
WHERE env = '<env>' AND resultado = 'erro' FACET service.name, tipo SINCE 3 hours ago
```

3. **Motivo no log**, pelo `mensagem.id` e pelo `os.id` gravados no MDC:

```sql
SELECT message, `os.id`, `saga.id`, `mensagem.id` FROM Log
WHERE message LIKE 'Falha ao processar%' SINCE 3 hours ago LIMIT 50
```

4. **Conteúdo da mensagem**, sem removê-la da DLQ (a visibilidade volta após 30 s):

```bash
URL=$(aws sqs get-queue-url --queue-name oficina-<env>-os-eventos-dlq --query QueueUrl --output text)
aws sqs receive-message --queue-url "$URL" --max-number-of-messages 5 \
  --message-attribute-names All --visibility-timeout 30
```

5. **Estado da saga** da OS citada no envelope:

```bash
curl -s "$API_URL/api/ordens/<osId>/saga" -H "Authorization: Bearer <token de funcionário>"
```

## Causas mais prováveis

| Sinal no log | Causa provável | Ação |
|---|---|---|
| `Mensagem fora do formato do envelope` | Contrato divergente do AsyncAPI no emissor | Corrigir o emissor; a mensagem pode ser descartada |
| `Evento ... recebido antes do esperado` | Evento chegou antes do anterior e o anterior se perdeu | Ver passos da saga; reenviar o evento faltante ou reprocessar após corrigir |
| `Saga da ordem de serviço não encontrado` | Evento de outro ambiente ou de OS removida | Descartar após confirmar a origem |
| `CannotGetJdbcConnectionException` | Banco indisponível durante as cinco tentativas | Recuperar o banco e reprocessar |

## Mitigação

- **Causa corrigida:** devolver as mensagens à fila de origem. O consumidor é idempotente
  por `mensagemId` (tabela `mensagens_processadas`), então reprocessar não duplica efeito.

```bash
DLQ_ARN=$(aws sqs get-queue-attributes --queue-url "$URL" --attribute-names QueueArn \
  --query Attributes.QueueArn --output text)
aws sqs start-message-move-task --source-arn "$DLQ_ARN"
```

- **Mensagem sem conserto** (formato inválido, OS inexistente): registrar o conteúdo no
  chamado e removê-la com `aws sqs delete-message`.
- **Saga parada em estado intermediário:** se o fluxo não puder continuar, seguir
  [saga-falha.md](./saga-falha.md) para a compensação manual.

## Quando escalar

Mais de uma mensagem na DLQ do mesmo tipo em uma hora, ou DLQ crescendo depois do
reprocessamento: indica defeito no consumidor ou no contrato, não falha transitória.
