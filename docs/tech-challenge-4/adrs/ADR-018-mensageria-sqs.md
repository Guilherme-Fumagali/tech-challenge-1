# ADR-018 — Amazon SQS como mensageria entre os serviços

- **Data:** 29/10/2026
- **Status:** Aceita

## Contexto

O enunciado pede mensageria assíncrona (RabbitMQ, Kafka, SQS ou similar) para eventos e integração desacoplada, e mensageria para a orquestração ou coreografia da saga.

As aulas apresentam três opções:

- **RabbitMQ:** usado no projeto de referência das aulas 4 e 5 de SAGA, com `spring-boot-starter-amqp` e Apache Camel.
- **Kafka:** citado na aula 2 de Estrutura de Microsserviços como exemplo de *message broker*.
- **Amazon SQS:** usado no *hands on* da aula 5 de Estrutura de Microsserviços (CQRS com LocalStack) e citado na aula 4 de SAGA como alternativa equivalente.

O tráfego da saga é baixo (dezenas de mensagens por OS), ponto a ponto (cada comando tem um destinatário) e precisa de reentrega em caso de falha. A infraestrutura existente está na AWS, com pipelines autenticados por OIDC e uma NAT instance para a saída das subnets privadas ([ADR-007](../../tech-challenge-3/adrs/ADR-007-rede-privada-nat-instance.md)).

## Decisão

As mensagens trafegam por filas **Amazon SQS padrão**, uma por serviço consumidor:

| Fila | Consumidor | Conteúdo |
|---|---|---|
| `oficina-<env>-execution-comandos` | Execution | comandos do orquestrador |
| `oficina-<env>-billing-comandos` | Billing | comandos do orquestrador |
| `oficina-<env>-os-eventos` | OS | respostas e eventos dos participantes |

- Cada fila tem DLQ, com `maxReceiveCount` 5 e visibilidade de 30 s.
- Criptografia SSE-SQS.
- Long polling de 20 s.
- Filas criadas pelo Terraform do serviço consumidor.
- Acesso por IAM da role do pod (EKS Pod Identity), sem usuário nem senha.
- Os serviços usam Spring Cloud AWS 3.3 (`@SqsListener` e `SqsTemplate`), compatível com a Spring Boot 3.4 da aplicação.
- Localmente e nos testes, o LocalStack, como no *hands on* da aula 5.

Como as filas padrão entregam pelo menos uma vez e sem garantia de ordem, todo consumidor é idempotente e o orquestrador trata eventos fora de ordem devolvendo a mensagem à fila ([ADR-020](./ADR-020-outbox-consumidor-idempotente.md)).

## Alternativas consideradas

**RabbitMQ no cluster EKS.** Fiel ao projeto de referência das aulas de SAGA e sem custo de serviço gerenciado. Exigiria um StatefulSet com volume persistente, o driver EBS CSI (ausente no cluster) e a operação do broker (usuários, filas, atualização). Descartada pela carga operacional.

**Amazon MQ para RabbitMQ.** Gerenciado e fiel às aulas. A menor instância custa cerca de US$ 20 a 25 por mês por broker e o provisionamento leva cerca de 20 minutos por ambiente, para um volume de mensagens que a SQS atende no nível gratuito. Descartada pelo custo e pelo tempo de provisionamento em um ambiente que é criado e destruído a cada sessão.

**Amazon MSK (Kafka).** Adequado a fluxos de eventos com retenção e reprocessamento. Para mensagens ponto a ponto de baixo volume, o custo mínimo do cluster e a operação de partições e grupos de consumidores não se justificam. Descartada.

**Filas FIFO.** Garantiriam ordem por OS (`MessageGroupId = osId`) e deduplicação. Descartadas porque a idempotência e o tratamento de eventos fora de ordem já são exigidos pelo desenho (inbox, *reread value*), e as filas padrão mantêm o código igual ao executado no LocalStack.

## Consequências

**Positivas**

- Nenhum broker para operar; custo praticamente nulo no volume do projeto.
- DLQ e métricas da fila nativas; mensagens de um serviço fora do ar aguardam até ele voltar.
- O agente New Relic propaga o contexto de trace nos atributos da mensagem, o que permite o trace distribuído atravessar a fila.

**Negativas**

- Sem pub/sub nativo: um evento consumido por mais de um serviço exigiria SNS. Na saga orquestrada isso não ocorre (todos os eventos vão ao orquestrador).
- O tráfego das subnets privadas para a SQS passa pela NAT instance, que já é ponto único de falha (DT-04).
- Desvio em relação à tecnologia do projeto de referência das aulas de SAGA (RabbitMQ).
