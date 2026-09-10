# RFC-002 — Escolha do banco de dados gerenciado

- **Autor:** Danilo Canato
- **Data:** 04/09/2026
- **Status:** Aceita → [ADR-010](../adrs/ADR-010-postgresql-rds.md)
- **Revisores:** Guilherme Fumagali Marques
- **Período de comentários:** 04/09 a 05/09/2026

## Contexto

O enunciado exige **banco de dados gerenciado** e, explicitamente, **justificativa formal para a escolha, com ajustes no modelo relacional, diagramas ER e explicação dos relacionamentos**.

O estado atual: PostgreSQL 16 em RDS `db.t4g.micro`, provisionado na Fase 2, com schema em cinco migrations Flyway.

O schema usa recursos específicos do PostgreSQL:

| Recurso | Onde |
|---|---|
| Extensão `pgcrypto` | `V1__schema_inicial.sql` |
| `UUID PRIMARY KEY DEFAULT gen_random_uuid()` | todas as tabelas |
| `NUMERIC(10,2)` para valores monetários | `servicos`, `pecas`, itens |
| `CHECK` nomeadas (`chk_status`, `chk_estoque_nao_negativo`) | `ordens_servico`, `pecas` |

O domínio é fortemente relacional: `clientes` 1—N `veiculos`, `veiculos` 1—N `ordens_servico`, e `ordens_servico` N—N com `servicos` e `pecas` por tabelas de itens.

Fator novo desta fase: a **Lambda de autenticação** consulta o banco a cada requisição de login, e escala por invocação.

## Proposta

**Manter PostgreSQL 16 em RDS.** A entrega documenta a escolha; não a refaz.

Ajustes cirúrgicos no modelo:

1. `clientes.status` — requisito direto, a Lambda precisa consultar "a existência **e o status**".
2. Índice `idx_clientes_cpf_status` — a consulta que a Lambda faz a cada autenticação.
3. Índice `idx_os_status_abertura` — sustenta a listagem por prioridade e as agregações dos dashboards.
4. `ordens_servico.data_ultima_transicao` — sem ela não há como medir permanência por status, insumo do dashboard exigido.

## Alternativas avaliadas

### Amazon DynamoDB

Escala melhor com Lambda, sem limite de conexões — o que resolveria elegantemente o risco de esgotamento do pool. Free tier generoso.

**Contra:** o domínio tem integridade referencial em cinco tabelas e junção em toda consulta de OS. Modelar isso em NoSQL exigiria desnormalizar e **reescrever a camada de persistência inteira**, sem ganho para a carga real do projeto. O custo da mudança é desproporcional ao problema que resolve.

### Amazon Aurora Serverless v2

Compatível com PostgreSQL, escala a zero, alta disponibilidade.

**Contra:** a capacidade mínima de 0,5 ACU custa mais que a `db.t4g.micro`, e a carga do projeto não justifica. Seria pagar por elasticidade que não será exercida.

### MySQL

Sem vantagem técnica, e o schema já depende de `gen_random_uuid()` e `pgcrypto`.

## Comentários recebidos

**Guilherme Fumagali:** *"O risco de conexões me preocupa mais que a escolha do engine. A `db.t4g.micro` suporta ~85 conexões e a Lambda escala por invocação. Precisamos de um número, não de uma intenção."*

**Resolução:** aceito, e virou controle explícito: `ReservedConcurrentExecutions = 10` na função, uma conexão por container de execução guardada em campo estático, sem pool, e `connectTimeout`/`socketTimeout` curtos. Se a concorrência precisar passar de 10, a resposta é **RDS Proxy**, não aumentar o limite. Registrado na SPEC-01 §7.

## Decisão

**PostgreSQL 16 em RDS mantido.** Formalizada em [ADR-010](../adrs/ADR-010-postgresql-rds.md).

## Consequências

- Zero migração de dados e zero reescrita de persistência.
- A justificativa formal descreve um sistema em produção, não uma hipótese.
- Limite de ~85 conexões é real e mitigado por concorrência reservada.
- Single-AZ sem failover automático, e `skip_final_snapshot` — decisões de custo para ambiente de estudo.
