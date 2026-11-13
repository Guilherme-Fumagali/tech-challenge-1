# ADR-020 — Transactional Outbox e consumidor idempotente

- **Data:** 29/10/2026
- **Status:** Aceita

## Contexto

Cada passo da saga altera o banco de um serviço e publica uma mensagem. Gravar no banco e publicar na fila são duas operações em sistemas diferentes. Quatro problemas decorrem disso:

1. Se a publicação acontece dentro da transação e o commit falha depois, uma mensagem anuncia um fato que não aconteceu.
2. Se a publicação acontece depois do commit e o processo cai entre os dois, o fato aconteceu e ninguém é avisado; a saga para.
3. A SQS padrão entrega cada mensagem pelo menos uma vez, então o consumidor pode receber a mesma mensagem mais de uma vez.
4. A SQS padrão não garante ordem entre mensagens.

O projeto de referência da aula 5 de SAGA envolve o `save` e o `publish` no mesmo `@Transactional`. Com um broker externo, a anotação controla a transação do banco, não a entrega da mensagem, e o problema 1 permanece. A aula 1 de Estrutura de Microsserviços II cita o *Transaction Outbox* entre os padrões para manter a consistência. A aula 5 de Estrutura de Microsserviços pede *event handlers* idempotentes, com base no identificador do evento.

Havia ainda um pré-requisito não atendido: o código do monólito não usava transações (cada `save` fazia commit sozinho). A reserva de peça, por exemplo, gravava o estoque e a OS em commits separados.

## Decisão

1. **Transação local explícita.** Cada serviço tem uma porta de aplicação `UnidadeDeTrabalho`, implementada com `TransactionTemplate` (JPA) ou com o `MongoTransactionManager`. Todo caso de uso que escreve executa leitura, validação, escrita e registro de mensagem dentro dela. `ordens_servico`, `pecas`, `orcamentos`, `pagamentos`, `sagas_ordem_servico` e o documento de execução têm trava otimista (`@Version`).
2. **Outbox.** O caso de uso não chama a SQS. A mensagem é gravada na tabela (ou coleção) `outbox` na mesma transação da mudança de estado.
3. **Relay.** Um job publica na SQS a cada 500 ms o que estiver pendente e marca como publicado. As linhas são reivindicadas com `FOR UPDATE SKIP LOCKED`; no MongoDB, com `findAndModify` e reserva por tempo. Assim o job pode rodar em várias réplicas sem publicar duas vezes a mesma linha ao mesmo tempo.
4. **Inbox.** O consumidor grava o `mensagemId` em `mensagens_processadas` na mesma transação do efeito. A segunda entrega da mesma mensagem viola a chave e é descartada.
5. **Idempotência por chave de negócio nos comandos.** O orquestrador reenvia comandos sem resposta com identificador novo. Por isso, o participante responde de novo ao comando repetido em vez de repetir o efeito: um segundo `GerarOrcamento` para a mesma OS devolve o orçamento já criado, e um `EstornarPagamento` sem pagamento aprovado responde estorno de valor zero.
6. **Ordem.** Evento que chega antes do estado esperado lança exceção, e a mensagem volta à fila após a visibilidade (30 s). Após cinco tentativas vai para a DLQ, que gera alerta.

## Alternativas consideradas

**Publicar após o commit (`TransactionSynchronization.afterCommit`).** Mais simples, mas perde a mensagem se o processo cair entre o commit e o envio. Descartada.

**Change Data Capture (Debezium, DynamoDB Streams).** Elimina o relay por *polling*, mas exige outro componente (conector Kafka Connect ou função Lambda) e não está no conteúdo das aulas. Descartada para o volume do projeto.

**Filas FIFO com deduplicação.** Deduplicação de apenas cinco minutos e só para envios repetidos, não para reprocessamentos legítimos; não substitui a inbox. Descartada ([ADR-018](./ADR-018-mensageria-sqs.md)).

## Consequências

**Positivas**

- Nenhum fato fica sem mensagem e nenhuma mensagem anuncia fato desfeito.
- Reentregas da SQS e reenvios do orquestrador não duplicam efeitos (orçamento, estorno, baixa de estoque).
- A adoção da unidade de trabalho corrige a falta de atomicidade da reserva de peças herdada do monólito.

**Negativas**

- Latência de até ~500 ms entre o commit e a publicação, mais o *long polling* do consumidor.
- Mais duas tabelas por serviço, um job permanente e limpeza periódica a planejar (outbox publicado e inbox antiga; no MongoDB a inbox tem TTL de 14 dias).
- O contexto de trace do New Relic precisa ser gravado na linha do outbox para que o trace distribuído continue no relay.
