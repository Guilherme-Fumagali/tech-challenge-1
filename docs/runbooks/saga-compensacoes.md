# Runbook — Pico de sagas compensadas

**Alerta:** `AWS-Oficina-<Amb>-Saga-Compensacoes-Warning`
**Dispara quando:** `sum(oficina.saga.compensadas)` acima de 5 em 15 minutos.

## O que significa

Mais ordens de serviço do que o normal terminaram canceladas pelo caminho de compensação. O
atributo `motivo` separa as causas:

| `motivo` | Gatilho | Compensações executadas |
|---|---|---|
| `REPROVACAO` | `OrcamentoReprovado` ou `OrcamentoExpirado` | `CancelarExecucao`, devolução das peças |
| `REJEICAO` | `ComandoRejeitado` para `GerarOrcamento` | `CancelarExecucao`, devolução das peças |
| `PAGAMENTO_RECUSADO` | `PagamentoRecusado` | `CancelarOrcamento`, `CancelarExecucao`, devolução das peças |
| `FALHA_EXECUCAO` | `ExecucaoFalhou` | `EstornarPagamento`, `CancelarOrcamento`, devolução das peças |

## Impacto

Cada compensação é uma OS perdida para a oficina. O sistema está funcionando: o alerta
existe porque um volume fora do padrão costuma ter causa comum.

## Diagnóstico

1. **Distribuição por motivo:**

```sql
SELECT sum(oficina.saga.compensadas) FROM Metric
WHERE env = '<env>' FACET motivo TIMESERIES AUTO SINCE 3 hours ago
```

2. **Por motivo:**
   - `PAGAMENTO_RECUSADO`: conferir os resultados no Billing.

     ```sql
     SELECT sum(oficina.pagamento.resultados) FROM Metric WHERE env = '<env>' FACET status SINCE 3 hours ago
     ```
   - `REJEICAO`: o motivo e o código do `ComandoRejeitado` estão no passo da saga
     (`GET /api/ordens/<osId>/saga`); `DADOS_INVALIDOS` costuma indicar cliente sem e-mail.
   - `FALHA_EXECUCAO`: conferir `oficina.execucao.falhas` e os apontamentos no Execution.
   - `REPROVACAO`: separar reprovação explícita de expiração (`OrcamentoExpirado`) pelos
     passos da saga.

## Mitigação

| Causa comum | Ação |
|---|---|
| Recusas em sequência no Mercado Pago | Ver [mercadopago-falhas.md](./mercadopago-falhas.md) |
| Cadastro incompleto do cliente | Corrigir o cadastro e abrir nova OS |
| Expiração em massa | Revisar a validade do orçamento (`ORCAMENTO_VALIDADE_HORAS` no Billing) |
| Falha de execução por falta de peça | Repor estoque antes de aceitar novas OS do mesmo serviço |

## Quando escalar

Quando o pico persistir por mais de uma hora com o mesmo `motivo`, ou quando `FALHA_EXECUCAO`
aparecer junto com estornos recusados (ver [saga-falha.md](./saga-falha.md)).
