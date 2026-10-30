# Runbook — Falhas em integrações

**Alerta:** `AWS-Oficina-<Amb>-MercadoPago-Falhas-Warning`
**Dispara quando:** `sum(oficina.integracao.mercadopago)` com `resultado != 'sucesso'` acima de 0.

## Mercado Pago

### O que significa

O Billing Service não concluiu uma chamada ao Mercado Pago: criação da cobrança, consulta de
pagamento ou estorno. O atributo `operacao` indica qual. As chamadas têm tempo limite, novas
tentativas e circuit breaker.

| Operação | Efeito na OS |
|---|---|
| Criação da cobrança | a aprovação do orçamento falha; a OS continua `AGUARDANDO_APROVACAO` e o cliente pode tentar de novo |
| Consulta | o pagamento fica pendente até a próxima reconciliação (a cada 60 s) |
| Estorno | o comando `EstornarPagamento` volta para a fila; após 5 falhas vai para a DLQ |

### Diagnóstico

```sql
SELECT sum(oficina.integracao.mercadopago) FROM Metric
WHERE env = '<env>' FACET operacao, resultado TIMESERIES AUTO SINCE 1 hour ago
```

Respostas 401 nos logs do Billing indicam `MP_ACCESS_TOKEN` inválido ou expirado. Para
indisponibilidade do provedor, consultar status.mercadopago.com.

### Mitigação

- **Provedor fora do ar:** aguardar; os pagamentos pendentes são reconciliados automaticamente.
- **Credencial inválida:** atualizar o segredo do repositório do Billing e refazer o deploy.
- **Estorno parado na DLQ:** reprocessar conforme [mensageria-dlq.md](./mensageria-dlq.md); se
  o estorno precisar ser feito no painel do Mercado Pago, seguir [saga-falha.md](./saga-falha.md).

Escalar em falhas de estorno, que deixam valor cobrado de uma OS cancelada, ou indisponibilidade
acima de 30 minutos.

## Acompanhamento da OS com fontes indisponíveis

Sem condição de alerta. O `GET /api/ordens/{id}/acompanhamento` consulta Billing e Execution
com limite de 2 s e circuit breaker; quando um deles falha, a resposta sai com o campo nulo e o
nome da fonte em `fontesIndisponiveis`. A saga não depende dessas chamadas.

```bash
kubectl get pods -n oficina -l app.kubernetes.io/name=billing-service
kubectl get pods -n oficina -l app.kubernetes.io/name=execution-service
```

Recuperar o participante. O circuito fecha sozinho depois de 30 s, se as chamadas de teste
tiverem sucesso.
