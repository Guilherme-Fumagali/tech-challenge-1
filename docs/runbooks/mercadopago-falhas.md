# Runbook — Falhas na integração com o Mercado Pago

**Alerta:** `AWS-Oficina-<Amb>-MercadoPago-Falhas-Warning`
**Dispara quando:** `sum(oficina.integracao.mercadopago)` com `resultado != 'sucesso'` acima de 0.

## O que significa

O Billing Service não concluiu uma chamada ao Mercado Pago: criação da preferência de
pagamento, consulta de pagamento, busca por `external_reference` ou estorno. O atributo
`operacao` indica qual. As chamadas usam timeout de 2 s na conexão e 5 s na leitura,
3 tentativas com backoff e circuit breaker.

## Impacto na saga

| Operação | Efeito na OS |
|---|---|
| Criação da preferência | A aprovação do orçamento falha; a OS continua `AGUARDANDO_APROVACAO` e o cliente pode tentar de novo |
| Consulta ou busca | O pagamento fica pendente até a próxima reconciliação (a cada 60 s); a OS continua `AGUARDANDO_PAGAMENTO` |
| Estorno | O comando `EstornarPagamento` volta para a fila e é repetido; após 5 falhas vai para a DLQ e a saga fica em `COMPENSANDO` |

## Diagnóstico

1. **Operação e resultado:**

```sql
SELECT sum(oficina.integracao.mercadopago) FROM Metric
WHERE env = '<env>' FACET operacao, resultado TIMESERIES AUTO SINCE 1 hour ago
```

2. **Log do Billing**, pelo `os.id`:

```sql
SELECT message, `os.id` FROM Log
WHERE service.name = 'billing-service' AND message LIKE '%Mercado Pago%' SINCE 1 hour ago LIMIT 50
```

3. **Estado do circuito** e status público do provedor (status.mercadopago.com).

4. **Credencial:** respostas 401 indicam `MP_ACCESS_TOKEN` inválido ou expirado no Secret
   `billing-service-secrets`.

## Mitigação

- **Indisponibilidade do provedor:** aguardar; os pagamentos pendentes são reconciliados
  automaticamente quando a consulta volta a responder.
- **Credencial inválida:** atualizar o segredo do repositório do Billing e refazer o deploy
  (o pipeline recria o Secret a partir do SSM).
- **Estorno parado na DLQ:** reprocessar conforme [mensageria-dlq.md](./mensageria-dlq.md);
  se o estorno precisar ser feito no painel do Mercado Pago, seguir [saga-falha.md](./saga-falha.md).

## Quando escalar

Falhas de estorno, que deixam valor cobrado de uma OS cancelada, ou indisponibilidade acima
de 30 minutos.
