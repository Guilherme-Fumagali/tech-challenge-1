# ADR-025 — API Composition com timeout e circuit breaker no acompanhamento da OS

- **Data:** 29/10/2026
- **Status:** Aceita

## Contexto

O cliente e o atendente precisam de uma visão única da OS: status e histórico (OS Service), orçamento e pagamento (Billing Service), etapa e apontamentos da execução (Execution Service). Com banco por serviço, nenhum serviço tem esses dados juntos.

A aula 5 de Estrutura de Microsserviços apresenta dois padrões:

- **API Composition:** chamadas paralelas aos serviços e agregação da resposta. É o preferido "pela facilidade de uso".
- **CQRS:** réplica local dos dados de outros serviços alimentada por eventos, para quando a composição não atende.

A aula 2 apresenta o Circuit Breaker para evitar que a lentidão de um serviço se propague a quem o chama.

## Decisão

O OS Service expõe `GET /api/ordens/{id}/acompanhamento`:

- Consulta em paralelo `GET /api/orcamentos?osId=` no Billing e `GET /api/execucoes/{osId}` no Execution.
- Repassa o JWT do chamador, e cada serviço aplica a própria regra de acesso (o cliente só vê a própria OS).
- Cada chamada tem limite de tempo de 2 s, circuit breaker (janela de 10 chamadas, abre com 50% de falha, 30 s aberto) e bulkhead de 10 chamadas simultâneas (Resilience4j).
- Fonte indisponível aparece como `null` e é listada em `fontesIndisponiveis`. A resposta é parcial, sem erro (*graceful degradation*).

O histórico de status continua local no OS, alimentado pelos eventos da saga, de modo que a parte principal do acompanhamento não depende dos outros serviços.

## Alternativas consideradas

**CQRS no OS (cópia de orçamento e execução por eventos).** A consulta não dependeria dos outros serviços, mas o OS passaria a manter dados que não são seus, com consistência eventual a explicar ao cliente. Descartada para esta consulta, de baixo volume; o histórico de status, que já chega pelos eventos da saga, cobre a parte que precisa estar sempre disponível.

**Composição no API Gateway.** O HTTP API da AWS não agrega respostas de várias integrações. Descartada.

**BFF dedicado.** Um quarto serviço só para composição, sem ganho com um único tipo de cliente. Descartada.

## Consequências

**Positivas**

- Demonstra comunicação síncrona entre serviços, com proteção contra falha em cascata.
- Billing ou Execution fora do ar não derrubam o acompanhamento: o cliente vê o status e o histórico.

**Negativas**

- O tempo de resposta é o da fonte mais lenta, limitado a 2 s.
- Os dados de orçamento e execução são lidos no momento da consulta, sem cache.
