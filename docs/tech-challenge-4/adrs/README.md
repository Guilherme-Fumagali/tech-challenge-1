# ADRs — Tech Challenge Fase 4

Registros de decisão arquitetural da Fase 4, no mesmo formato e com a mesma numeração sequencial da Fase 3 ([índice da Fase 3](../../tech-challenge-3/adrs/README.md)).

## Índice

| ADR | Decisão | Status |
|---|---|---|
| [016](./ADR-016-decomposicao-tres-microsservicos.md) | Decomposição em três microsserviços por subdomínio (Strangler Fig) e composição de API | Aceita |
| [017](./ADR-017-saga-orquestrada.md) | Saga orquestrada pelo OS Service | Aceita |
| [018](./ADR-018-mensageria-sqs.md) | Amazon SQS como mensageria entre os serviços | Aceita |
| [019](./ADR-019-banco-por-servico.md) | Banco por serviço: PostgreSQL, MongoDB (DocumentDB) e DynamoDB | Aceita |
| [020](./ADR-020-outbox-consumidor-idempotente.md) | Transactional Outbox e consumidor idempotente | Aceita |
| [021](./ADR-021-mercado-pago-checkout-pro.md) | Mercado Pago Checkout Pro com webhook assinado e reconciliação | Aceita |
| [022](./ADR-022-infra-por-servico-pod-identity.md) | Infraestrutura por serviço, identidade de pod e roteamento na borda | Aceita |
| [023](./ADR-023-segundo-fator-autenticacao.md) | Segundo fator na autenticação e desacoplamento do banco do OS | Aceita |

## Relação com as ADRs da Fase 3

| ADR anterior | Efeito |
|---|---|
| ADR-001 · Quatro repositórios | **Superada pela ADR-022** quanto aos manifestos e ao banco fora do repositório da aplicação; a ADR-016 acrescenta os repositórios dos novos serviços |
| ADR-003 · Emissão de JWT pela Lambda | complementada pela ADR-023 (segundo fator) |
| ADR-004 · API Gateway HTTP API | complementada pela ADR-022 (rotas por serviço) |
| ADR-007 · NAT instance | mantida; o tráfego para SQS e SES passa por ela (DT-19) |
| ADR-010 · PostgreSQL em RDS | complementada pela ADR-019 (banco por serviço, NoSQL no Execution) |
| ADR-013 · Pipelines com role própria | mantida e estendida às duas roles novas |

## Processo

O mesmo da Fase 3: a ADR nasce como Proposta, é revisada pela equipe, passa a Aceita e não é mais editada; mudança de rumo gera uma ADR nova, que marca a anterior como Superada.
