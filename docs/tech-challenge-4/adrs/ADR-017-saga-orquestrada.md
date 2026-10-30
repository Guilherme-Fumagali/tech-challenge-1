# ADR-017 — Saga orquestrada pelo OS Service

- **Data:** 29/10/2026
- **Status:** Aceita

## Contexto

Com o banco dividido entre três serviços ([ADR-016](./ADR-016-decomposicao-tres-microsservicos.md)), o fluxo da ordem de serviço deixa de caber em uma transação ACID. O enunciado exige o padrão Saga, com rollback e compensação em caso de falha em qualquer etapa, e pede que a escolha entre orquestração e coreografia seja justificada.

As aulas de SAGA apresentam as duas estratégias:

- **Coreografia:** simples e com acoplamento fraco, mas sem um ponto que descreva a saga inteira, com risco de dependências cíclicas e de cada participante precisar assinar muitos eventos.
- **Orquestração:** dependências em mão única e lógica de coordenação centralizada, ao custo de um orquestrador que pode concentrar complexidade e ser ponto único de falha.

A aula 2 registra a recomendação de Richardson: coreografia para sagas simples, orquestração nas demais.

A saga da oficina não é simples:

- Tem sete etapas.
- Duas esperas por pessoas: decisão do cliente e pagamento.
- Atividades do mecânico que podem levar dias.
- Três caminhos de falha com compensações em ordens diferentes.
- Um passo pivô (pagamento aprovado) a partir do qual desfazer exige estorno no gateway de pagamento.

## Decisão

A saga é **orquestrada**, e o orquestrador fica no OS Service, dono da ordem de serviço — o mesmo arranjo do projeto de referência da aula 5 (`SagaPedido` no serviço de pedidos).

- O estado da saga é persistido no banco do OS (`sagas_ordem_servico` e `saga_passos`), não em memória. Uma réplica que reinicia retoma a saga a partir do último estado gravado.
- A lógica de transição é uma classe de domínio sem dependência de framework (`SagaOrdemServico`): recebe um evento e devolve a decisão (transição da OS, comandos a enviar, compensação local). Cada linha da tabela de transições tem um teste unitário.
- A comunicação é assíncrona: o orquestrador envia comandos para as filas dos participantes e recebe respostas e eventos em uma fila própria, equivalente à `orquestracao_pedidos_saga_reply` da aula 5.
- As compensações seguem a contramedida *pessimistic view* da aula 3: primeiro o que impede trabalho novo (cancelar orçamento, cancelar execução), por último o que devolve recurso (estornar pagamento, estornar estoque).
- Antes de aplicar cada evento, o orquestrador relê a OS e a saga com trava otimista (`@Version`), aplicando a contramedida *reread value*.
- Comandos sem resposta dentro do prazo são reenviados com identificador novo; acima de três reenvios a saga vai para `FALHA` e gera alerta.

O contrato completo (estados, transições, compensações e prazos) está na SPEC-08 do plano de implementação e documentado no README do OS Service e no `docs/asyncapi.yaml` de cada serviço.

## Alternativas consideradas

**Coreografia.** Cada serviço reagiria aos eventos dos outros. Para os três caminhos de falha, cada participante precisaria conhecer a ordem das compensações dos demais, e não haveria lugar único para consultar em que ponto está a saga de uma OS. Descartada pela complexidade do fluxo, conforme a recomendação da aula 2.

**AWS Step Functions.** Citada nas aulas 2 e 4 como forma de implementar a saga orquestrada. Acrescentaria um serviço gerenciado à arquitetura, exigiria *callback tokens* para as esperas humanas e tiraria a lógica da saga do alcance dos testes unitários e do BDD executados no pipeline. Descartada.

**Apache Camel, como no projeto de referência.** Resolveria o roteamento de mensagens, mas a lógica de compensação ficaria em rotas declarativas difíceis de testar unitariamente, e o projeto de referência usa RabbitMQ ([ADR-018](./ADR-018-mensageria-sqs.md)). Descartada.

**Orquestrador como quarto serviço.** Isolaria a coordenação, mas separaria o estado da saga do estado da OS, que mudam juntos em quase todos os passos. Descartada.

## Consequências

**Positivas**

- A saga inteira é lida em um único lugar (tabela de transições e classe de domínio) e consultada por OS em `GET /api/ordens/{id}/saga`.
- Billing e Execution não conhecem a saga: recebem comandos e publicam eventos.
- O BDD do OS cobre o fluxo completo e as compensações com os participantes simulados.

**Negativas**

- O OS Service concentra a coordenação; sua indisponibilidade pausa o avanço das sagas (as mensagens aguardam na fila, sem perda).
- Mais um conjunto de tabelas e um job de prazos no OS.
- Sem isolamento entre sagas da mesma OS; as contramedidas reduzem, mas não eliminam, anomalias de leitura durante uma compensação em andamento.
