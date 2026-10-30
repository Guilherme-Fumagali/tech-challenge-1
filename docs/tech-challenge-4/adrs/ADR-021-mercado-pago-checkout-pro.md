# ADR-021 — Mercado Pago Checkout Pro com webhook assinado e reconciliação

- **Data:** 29/10/2026
- **Status:** Aceita

## Contexto

O enunciado exige que a parte de pagamentos do Billing Service se integre ao Mercado Pago. A aprovação do pagamento é o passo pivô da saga ([ADR-017](./ADR-017-saga-orquestrada.md)): depois dele, desfazer exige estorno no gateway.

O Mercado Pago oferece:

- **Checkout Pro:** preferência de pagamento criada no backend; o cliente paga em página do próprio Mercado Pago.
- **Checkout Transparente / API de Orders:** o formulário de cartão fica no site da loja, com tokenização no navegador.

O resultado do pagamento chega por notificação (webhook) e pode ser consultado pela API de pagamentos.

Três restrições do ambiente:

- A URL pública do API Gateway muda a cada provisionamento.
- O projeto não tem frontend.
- Pela documentação, pagamentos feitos com credenciais de teste não disparam notificações. O teste ponta a ponta usa contas de teste (vendedor e comprador) e as credenciais do vendedor de teste.

## Decisão

- **Checkout Pro**, integrado por `RestClient` à API REST, sem o SDK Java:
  - `POST /checkout/preferences` com `external_reference` igual ao id do orçamento.
  - `binary_mode: true` (aprovado ou recusado, sem pendente).
  - Validade igual ao prazo de pagamento.
  - `notification_url` montada a cada preferência a partir da URL pública atual do gateway, o que dispensa editar o painel a cada provisionamento.
- **Webhook** em `POST /api/pagamentos/webhooks/mercado-pago`:
  - Rota pública no API Gateway.
  - Valida a assinatura `x-signature`: HMAC-SHA256 do manifesto `id:{data.id};request-id:{x-request-id};ts:{ts};` com o segredo do painel, comparação em tempo constante.
  - Não confia no corpo da notificação: o status vem sempre de `GET /v1/payments/{id}` com o token do vendedor.
- **Reconciliação:** job a cada 60 s busca por `external_reference` os pagamentos pendentes há mais de um minuto. Cobre notificação perdida, atrasada ou não enviada, como no caso das credenciais de teste.
- **Estorno:** `POST /v1/payments/{id}/refunds` como compensação do passo pivô quando a execução falha.
- Criação de preferência e estorno com `X-Idempotency-Key` (id do orçamento e id do pagamento), retry com backoff exponencial em erro de rede/5xx e circuit breaker (Resilience4j).
- **Gateway simulado** no perfil `local` (`GatewayPagamentoSimulado`), para executar a saga completa sem a conta do Mercado Pago nos testes e no compose.
- Regra de recusa: a primeira recusa encerra a cobrança e dispara a compensação. Um pagamento aprovado que chegue depois para orçamento já cancelado é estornado automaticamente.

## Alternativas consideradas

**Checkout Transparente / API de Orders.** Exige tokenização do cartão no frontend com o SDK JavaScript do Mercado Pago e manipulação de dados de cartão na página da oficina. Sem frontend no projeto, descartada.

**SDK Java do Mercado Pago.** Configuração estática global do token (`MercadoPagoConfig`), o que dificulta testes isolados, e mais uma dependência para manter. Com quatro chamadas REST, o `RestClient` testado com WireMock é suficiente. Descartada.

**Somente webhook, sem reconciliação.** A notificação pode não chegar (credencial de teste, indisponibilidade do serviço durante as retentativas do Mercado Pago). A saga ficaria parada em `AGUARDANDO_PAGAMENTO`. Descartada.

**Pix.** Em ambiente de teste o Pix permanece pendente até a ação manual; o cartão de teste com os nomes `APRO` (aprovado) e `OTHE` (recusado) permite demonstrar os dois caminhos da saga. O Checkout Pro continua oferecendo Pix ao comprador; a demonstração usa cartão.

## Consequências

**Positivas**

- O fluxo de pagamento é demonstrável de ponta a ponta com o sandbox: aprovação, recusa com compensação e estorno visível no painel.
- A saga não depende de a notificação chegar.
- Nenhum dado de cartão passa pelos serviços da oficina.

**Negativas**

- O cliente sai do fluxo da oficina para pagar e volta por `back_urls`.
- A conta, a aplicação e as contas de teste do Mercado Pago são pré-requisitos manuais do ambiente (roteiro em `oficina-billing-service/docs/mercado-pago.md`).
- O token de acesso do vendedor é um segredo de longa duração armazenado no SSM; sua rotação é manual.
