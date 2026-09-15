# RFC-002 — Escolha do banco de dados gerenciado

- **Autor:** Danilo Canato
- **Data:** 04/09/2026
- **Status:** Aceita → [ADR-010](../adrs/ADR-010-postgresql-rds.md)
- **Revisores:** Guilherme Fumagali Marques
- **Período de comentários:** 04/09 a 05/09/2026

## Contexto

O enunciado exige banco de dados gerenciado e, explicitamente, justificativa formal para a escolha, com ajustes no modelo relacional, diagramas ER e explicação dos relacionamentos.

O estado atual é PostgreSQL 16 em RDS `db.t4g.micro`, provisionado na Fase 2, com schema em cinco migrations Flyway.

O schema usa recursos específicos do PostgreSQL:

| Recurso | Onde |
|---|---|
| Extensão `pgcrypto` | `V1__schema_inicial.sql` |
| `UUID PRIMARY KEY DEFAULT gen_random_uuid()` | todas as tabelas |
| `NUMERIC(10,2)` para valores monetários | `servicos`, `pecas`, itens |
| `CHECK` nomeadas (`chk_status`, `chk_estoque_nao_negativo`) | `ordens_servico`, `pecas` |

O domínio é fortemente relacional: `clientes` 1:N `veiculos`, `veiculos` 1:N `ordens_servico`, e `ordens_servico` N:N com `servicos` e `pecas` por tabelas de itens.

Um fator novo nesta fase é a Lambda de autenticação, que consulta o banco a cada requisição de login e escala por invocação.

## Proposta

Manter PostgreSQL 16 em RDS. A entrega documenta a escolha existente, sem refazê-la.

Ajustes pontuais no modelo:

1. `clientes.status`: requisito direto, pois a Lambda precisa consultar "a existência e o status".
2. Índice `idx_clientes_cpf_status`: atende à consulta que a Lambda executa a cada autenticação.
3. Índice `idx_os_status_abertura`: sustenta a listagem por prioridade e as agregações dos dashboards.
4. `ordens_servico.data_ultima_transicao`: sem essa coluna não é possível medir a permanência por status, insumo do dashboard exigido.

## Alternativas avaliadas

### Amazon DynamoDB

Escala melhor com Lambda e não tem limite de conexões, o que eliminaria o risco de esgotamento do pool. Oferece free tier generoso.

**Contra:** o domínio tem integridade referencial em cinco tabelas e junção em toda consulta de OS. Modelar isso em NoSQL exigiria desnormalizar os dados e reescrever toda a camada de persistência, sem ganho para a carga efetiva do projeto. O custo da mudança é desproporcional ao problema que ela resolve.

### Amazon Aurora Serverless v2

Compatível com PostgreSQL, escala a zero e oferece alta disponibilidade.

**Contra:** a capacidade mínima de 0,5 ACU custa mais que a `db.t4g.micro`, e a carga do projeto não justifica esse custo, que corresponderia a uma elasticidade não utilizada.

### MySQL

Não apresenta vantagem técnica, e o schema já depende de `gen_random_uuid()` e `pgcrypto`.

## Comentários recebidos

**Guilherme Fumagali:** *"O risco de conexões me preocupa mais que a escolha do engine. A `db.t4g.micro` suporta ~85 conexões e a Lambda escala por invocação. É preciso definir um limite numérico para esse controle."*

**Resolução:** aceito e convertido em controle explícito: teto de 10 execuções simultâneas da função, uma conexão por container de execução mantida em campo estático, sem pool, e `connectTimeout`/`socketTimeout` curtos. Na conta utilizada, o teto é imposto pelo limite total de 10 execuções simultâneas de Lambda, que também impede reservar concorrência; o template aceita a reserva pelo parâmetro `ConcorrenciaReservada` quando a cota for ampliada. Caso a concorrência precise ultrapassar 10, a solução prevista é o RDS Proxy, em vez do aumento do limite. Registrado na SPEC-01 §7.

## Decisão

Mantido o PostgreSQL 16 em RDS, conforme formalizado em [ADR-010](../adrs/ADR-010-postgresql-rds.md).

## Consequências

- Não há migração de dados nem reescrita da camada de persistência.
- A justificativa formal descreve um sistema já em produção.
- O limite de ~85 conexões é efetivo e foi mitigado pelo teto de 10 execuções simultâneas da Lambda.
- Single-AZ sem failover automático e `skip_final_snapshot`, ambas decisões de custo para ambiente de estudo.
