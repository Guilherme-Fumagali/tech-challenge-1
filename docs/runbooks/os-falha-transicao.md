# Runbook — Falha na transição de status de OS

**Alerta:** `AWS-OficinaAPI-<Amb>-OS-FalhaTransicao-Critical`
**Dispara quando:** mais de 5 falhas de transição em 5 minutos.

## O que significa

A métrica `oficina.os.transicoes` com `resultado = 'falha'` é incrementada pelo
`GlobalExceptionHandler` sempre que uma `TransicaoInvalidaException` é lançada — alguém
tentou mover uma OS para um status que a máquina de estados não permite a partir do
estado atual.

Transições válidas:

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                       └──────────→ CANCELADA
```

## Impacto

Ordens de serviço travadas: atendente não consegue avançar o fluxo, cliente não recebe
orçamento nem retirada. **É o requisito de negócio da fase — falha no processamento de OS.**

## Diagnóstico

1. Quais ordens e quais transições:

```sql
SELECT sum(oficina.os.transicoes) FROM Metric
WHERE resultado = 'falha' FACET motivo, status_destino SINCE 30 minutes ago
```

2. Correlacionar com o log estruturado, que carrega `ordem.id` no MDC:

```sql
SELECT message, `ordem.id`, `trace.id` FROM Log
WHERE `log.level` = 'WARN' AND message LIKE '%Transição inválida%' SINCE 30 minutes ago
```

3. Do log, saltar para o **trace distribuído** pelo `trace.id` e ver a sequência completa
   da requisição.

4. Estado real da ordem no banco:

```sql
SELECT id, status, data_ultima_transicao FROM ordens_servico WHERE id = '<ordem.id>';
```

## Causas mais prováveis

| Sintoma | Causa provável | Ação |
|---|---|---|
| Muitas falhas na **mesma** ordem | Retentativa de cliente ou duplo clique na UI | Verificar idempotência do endpoint; não é bug de servidor |
| Falhas espalhadas em ordens diferentes, mesmo `status_destino` | Regressão na máquina de estados após deploy | Conferir o último deploy e considerar rollback |
| Pico logo após deploy | Migration incompleta ou dado inconsistente | Conferir Flyway: `SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;` |
| Falhas concentradas em `aprovar-externo` | Token de aprovação expirado sendo reusado | Esperado — validar se o volume justifica ajustar `APROVACAO_TOKEN_VALIDADE_HORAS` |

## Mitigação

- **Regressão confirmada:** rollback do deploy.
  ```bash
  kubectl rollout undo deployment/oficina-api -n oficina
  kubectl rollout status deployment/oficina-api -n oficina
  ```
- **Dado inconsistente numa ordem específica:** corrigir o status com registro do motivo,
  nunca em massa.
- **Ruído de retentativa:** ajustar o limiar do alerta, não silenciá-lo.

## Quando escalar

Se a taxa não cair após rollback, ou se houver ordens em estado que a máquina de estados
não deveria produzir — indica escrita fora da aplicação.
