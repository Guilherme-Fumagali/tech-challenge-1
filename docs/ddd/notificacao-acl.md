# Serviço de notificação e Anti-Corruption Layer

O envio do orçamento ao cliente é feito por um serviço de notificação, modelado no Event Storming como sistema externo. Este documento registra como a integração foi isolada do domínio e onde ela está depois da divisão em microsserviços.

## Histórico

Na Fase 1, o Event Storming registrou o serviço de notificação como **Hot Spot**: o canal (e-mail, SMS ou push) ainda não estava definido. O MVP implementou apenas a fronteira da integração e um adaptador que registrava a notificação em log.

Na Fase 2, foi implementado o envio de e-mail por SMTP (`SmtpNotificacaoService`, com MailHog nos ambientes de desenvolvimento e demonstração), incluindo os links de aprovação externa.

Na Fase 4, o orçamento, a aprovação e o e-mail saíram do monólito para o **Billing Service** (Strangler Fig). O OS Service deixou de ter porta de notificação, adaptador SMTP e propriedades de e-mail; ele envia ao Billing o comando `GerarOrcamento` com o snapshot dos itens e os dados de contato do cliente, e o Billing envia o e-mail com os links de decisão.

## Onde fica o ACL na Fase 4

| Fronteira | Serviço | Implementação |
|---|---|---|
| Notificação ao cliente (e-mail com links de decisão e de pagamento) | Billing Service | porta `NotificacaoCliente`, adaptadores SMTP e log escolhidos por `app.notificacao.canal` |
| Mercado Pago | Billing Service | adaptador `MercadoPagoGateway` atrás da porta `GatewayPagamento` |
| Eventos dos participantes recebidos pelo orquestrador | OS Service | `infrastructure/messaging/TradutorEventos`, que converte o envelope da SPEC-08 em `EventoSaga` do domínio |
| Comandos enviados aos participantes | OS Service | `application/usecase/saga/MontadorDadosComando` e a porta `PublicadorMensagens` |

No OS Service, o domínio da saga (`domain/saga`) não conhece SQS, JSON nem nomes de fila. O `TradutorEventos` lê os campos do contrato (`etapa`, `comando`, instante do fato de cada evento) e entrega ao domínio um `EventoSaga` com tipos próprios; mudanças no formato da mensagem ficam restritas a essa classe.

```
SQS os-eventos
  └─ ConsumidorEventosOs (infraestrutura)
       └─ TradutorEventos: MensagemEnvelope → EventoSaga      fronteira de entrada
            └─ ProcessarEventoSagaUseCase (aplicação)
                 └─ SagaOrdemServico.aplicar (domínio)
                 └─ MontadorDadosComando + PublicadorMensagens  fronteira de saída
                      └─ PublicadorOutbox → tabela outbox → RelayOutbox → SQS
```

## Arquivos no OS Service

| Arquivo | Camada | Papel |
|---|---|---|
| `infrastructure/messaging/TradutorEventos.java` | infraestrutura | tradução dos eventos recebidos |
| `application/port/PublicadorMensagens.java` | aplicação | fronteira de saída dos comandos |
| `application/usecase/saga/MontadorDadosComando.java` | aplicação | dados de cada comando na linguagem do contrato |
| `infrastructure/messaging/PublicadorOutbox.java` | infraestrutura | grava o envelope no outbox |

A descrição do adaptador SMTP e da página de decisão passou para o README do repositório `oficina-billing-service`.
