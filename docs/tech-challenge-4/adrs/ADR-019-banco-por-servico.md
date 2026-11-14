# ADR-019 — Banco de dados por serviço: PostgreSQL, MongoDB (DocumentDB) e DynamoDB

- **Data:** 29/10/2026
- **Status:** Aceita
- **Complementa:** [ADR-010](../../tech-challenge-3/adrs/ADR-010-postgresql-rds.md) (PostgreSQL em RDS), cuja rejeição de NoSQL valia para o domínio único da Fase 3

## Contexto

O enunciado exige banco próprio para cada microsserviço, com pelo menos um banco relacional e pelo menos um não relacional, e proíbe que um serviço acesse diretamente o banco de outro.

As aulas tratam o tema em três pontos:

- **Estrutura de Microsserviços, aula 5:** o banco compartilhado é anti-padrão; o banco por serviço garante baixo acoplamento ao custo de exigir API Composition, CQRS e Saga.
- **Estrutura de Microsserviços, aula 1:** a separação pode ser "logicamente apenas, não é necessário que cada banco de dados tenha uma instância física dedicada".
- **Data Engineering, aula 7:** cada serviço pode ter seu banco e cada banco pode ser de um tipo diferente.

A aula 1 de Data Engineering associa o modelo relacional a transações ACID e consistência rígida, e o NoSQL a esquema flexível e escala horizontal. A aula 2 é a única com modelagem de NoSQL em profundidade (MongoDB). As aulas 3 a 5 cobrem Redis, Cassandra e Neo4j, e a aula 6 os serviços gerenciados da AWS: RDS, DynamoDB, DocumentDB "compatível com MongoDB" e Neptune.

## Decisão

| Serviço | Banco | Instância |
|---|---|---|
| OS Service | PostgreSQL 16 | RDS `oficina-os-db-<env>` (`db.t4g.micro`) |
| Billing Service | PostgreSQL 16 | RDS `oficina-billing-db-<env>` (`db.t4g.micro`), instância própria |
| Execution Service | MongoDB (API 5.0) | Amazon DocumentDB `oficina-execution-docdb-<env>` (`db.t4g.medium`) |
| Autenticação (2º fator) | DynamoDB | tabela `oficina-auth-desafios-<env>`, sob demanda, com TTL |

Cada serviço recebe apenas as próprias credenciais (prefixo SSM `/oficina/<env>/<serviço>/`), e o Terraform de cada banco está no repositório do serviço dono ([ADR-022](./ADR-022-infra-por-servico-pod-identity.md)).

### Justificativa por serviço

**OS Service — PostgreSQL.**

- Concentra entidades relacionadas: cliente, veículo, catálogo, peça com estoque, OS e itens.
- As invariantes estão em constraints: estoque não negativo, CPF/CNPJ e placa únicos.
- A reserva de peça precisa alterar a peça e a OS na mesma transação.
- O relatório de tempo médio é uma consulta agregada.
- É o banco já escolhido e justificado na Fase 1 e na [ADR-010](../../tech-challenge-3/adrs/ADR-010-postgresql-rds.md).

**Billing Service — PostgreSQL em instância própria.**

- Dados financeiros: um orçamento por OS, um pagamento aprovado por orçamento, identificador do pagamento no Mercado Pago único para que o webhook seja idempotente, trilha de notificações recebidas e consultas de conciliação por status e data.
- Essas regras ficam em constraints (`UNIQUE`, `CHECK`) e em transações.
- O corpo devolvido pelo Mercado Pago, semiestruturado, é guardado em coluna `JSONB`.
- A instância separada da do OS isola falha, carga e credenciais. O custo adicional é de cerca de US$ 0,02/h com o ambiente ligado.

**Execution Service — MongoDB no Amazon DocumentDB.**

- O agregado Execução é lido e escrito inteiro por OS e reúne listas que crescem durante o trabalho: etapas, apontamentos do mecânico, histórico de status e itens do reparo, com conteúdo que varia de uma OS para outra.
- É o caso descrito na aula 2 de Data Engineering: dados semiestruturados que "variam em sua estrutura ao longo do tempo", com os relacionamentos incorporados no documento.
- A fila é atendida por um índice composto `{status, prioridade, enfileiradaEm}`.
- A atualização de um documento é atômica.
- As transações multi-documento (DocumentDB 4.0 em diante) garantem o outbox na mesma transação do agregado.
- O DocumentDB mantém o banco na VPC, com o mesmo modelo de security group, criptografia e backup do RDS. Esses são os benefícios de DBaaS listados na aula 6.

**Autenticação — DynamoDB.**

- O desafio do segundo fator é acessado só pela chave, vive cinco minutos e precisa expirar sozinho.
- Uma tabela chave-valor sob demanda com TTL atende sem servidor ligado, como o próprio Lambda.

## Alternativas consideradas

| Alternativa | Motivo da rejeição |
|---|---|
| Um RDS com bancos lógicos por serviço | Permitida pela aula 1, mas o enunciado fala em infraestrutura e banco próprios; a criação de bancos dentro de uma instância privada não é possível pelo provedor AWS do Terraform sem acesso de rede a partir do runner; e uma única instância concentraria falha e conexões (limite de ~85 no `db.t4g.micro`) |
| MongoDB no Billing (exemplo `bills` da aula 2) | O formato de documento atenderia, mas trocaria constraints relacionais por regras de aplicação justamente no serviço de dinheiro, que também é o de lógica mais complexa (gateway, webhook, estorno) |
| Redis na execução (aula 3, listas e *sorted sets*) | Tratado nas aulas principalmente como cache, com persistência opcional; não serve como registro de negócio de uma OS em reparo |
| Cassandra (aula 4) | Cluster distribuído, modelagem orientada a consultas e operação especializada, desproporcionais ao volume de uma oficina |
| Neo4j (aulas de Grafos) | A execução não é dominada por relacionamentos entre entidades; alta disponibilidade só na edição Enterprise. Fica registrado como possibilidade de *read model* analítico (peças por modelo de veículo) |
| DynamoDB na execução | Sem conteúdo de modelagem nas aulas; consultas da fila dependeriam do desenho de chaves e índices secundários, com menos flexibilidade que a linguagem de consulta do MongoDB |
| MongoDB Atlas | Fora da VPC, exigiria liberar o IP da NAT e outra conta com outro faturamento |
| MongoDB em StatefulSet no EKS | Exigiria o driver EBS CSI, volumes persistentes e a operação de backup e atualização do banco dentro do cluster |
| Redis/ElastiCache para os desafios do 2º fator | Cluster pago ligado o tempo todo na VPC para um dado que vive cinco minutos |

## Consequências

**Positivas**

- Persistência poliglota alinhada ao conteúdo de Data Engineering: relacional onde há invariantes e transação multi-entidade, documento onde o agregado é semiestruturado, chave-valor com expiração onde o acesso é por chave.
- Nenhum serviço conhece as credenciais do banco de outro.

**Negativas**

- Três tecnologias de persistência para operar e testar (Testcontainers com PostgreSQL e MongoDB, LocalStack com DynamoDB).
- O DocumentDB é compatível com a API do MongoDB 5.0, não idêntico: os testes usam MongoDB 6.0 e o código se limita a CRUD, índices, TTL e transações. Exige TLS com o certificado da AWS no truststore da imagem e `retryWrites=false`.
- Todos os pods compartilham o security group do cluster; o isolamento entre serviços é por credencial e não por rede (débito registrado).
- Custo com o ambiente ligado sobe cerca de US$ 0,09/h (segundo RDS e DocumentDB).
