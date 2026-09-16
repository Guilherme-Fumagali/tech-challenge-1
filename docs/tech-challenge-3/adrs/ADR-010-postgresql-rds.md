# ADR-010 — PostgreSQL em RDS mantido

- **Data:** 04/09/2026
- **Status:** Aceita
- **RFC de origem:** RFC-002 (escolha do banco)

## Contexto

O enunciado exige banco de dados gerenciado (PostgreSQL, MySQL, SQL Server ou outro) e justificativa formal para a escolha do banco, com ajustes no modelo relacional, diagramas ER e explicação dos relacionamentos.

A Fase 2 já provisionou `aws_db_instance.oficina`: PostgreSQL 16, `db.t4g.micro`, 20 GB gp3, `publicly_accessible = false`. O schema está versionado em cinco migrations Flyway (`V1`–`V5`) e usa recursos específicos do PostgreSQL: extensão `pgcrypto`, `UUID PRIMARY KEY DEFAULT gen_random_uuid()`, `NUMERIC(10,2)` para valores monetários e `CHECK` constraints nomeadas, entre elas `chk_status`, que espelha o enum `StatusOS` no banco.

O domínio é fortemente relacional: `clientes` 1:N `veiculos`, `veiculos` 1:N `ordens_servico`, e `ordens_servico` N:N com `servicos` e `pecas` por tabelas de itens. Há invariantes de integridade protegidas pelo banco, como `chk_estoque_nao_negativo`.

## Decisão

Manter PostgreSQL 16 em Amazon RDS. A entrega documenta a escolha existente, sem refazê-la.

Ajustes no modelo relacional nesta fase:

1. **`clientes.status`:** `VARCHAR(20) NOT NULL DEFAULT 'ATIVO'` com `CHECK (status IN ('ATIVO','INATIVO','BLOQUEADO'))`. Requisito direto: a Lambda precisa consultar "a existência e o status do cliente".
2. **Índice `idx_os_status_abertura`** em `ordens_servico(status, data_abertura)`: sustenta a listagem ordenada por status exigida desde a Fase 2 e as agregações dos dashboards.
3. **Índice `idx_clientes_cpf_status`** em `clientes(cpf_cnpj, status)`: atende à consulta que a Lambda executa a cada autenticação.

## Alternativas consideradas

**DynamoDB** (Aula 01 de Serverless). Escala melhor com Lambda, não tem limite de conexões e oferece free tier generoso. Descartada porque o domínio é relacional, com integridade referencial em cinco tabelas e junções em toda consulta de OS. Modelar isso em NoSQL exigiria desnormalização e reescrita completa da camada de persistência, sem ganho para a carga efetiva do projeto.

**Aurora Serverless v2** (Aula 01 de Serverless). Compatível com PostgreSQL, escala a zero e oferece alta disponibilidade. Descartada por custo: a capacidade mínima de 0,5 ACU custa mais que a `db.t4g.micro`, e a carga do projeto não justifica.

**MySQL.** Sem vantagem, e o schema já usa `gen_random_uuid()` e `pgcrypto`, específicos do PostgreSQL.

**Continuar com PostgreSQL em StatefulSet no cluster** (`k8s/database/`). Descartada por violar o requisito de banco gerenciado. Os manifests permanecem apenas para o ambiente local `kind`.

## Consequências

**Positivas**
- Não há migração de dados nem reescrita da camada de persistência.
- A justificativa formal exigida descreve um sistema já em produção.
- `db.t4g.micro` é elegível ao free tier em conta nova.
- As constraints no banco funcionam como segunda linha de defesa das invariantes de domínio.

**Negativas**
- **Limite de conexões.** `db.t4g.micro` suporta cerca de 85 conexões simultâneas. Com a aplicação em até 3 pods e a Lambda escalando por invocação, esse limite pode ser atingido. Mitigação em [SPEC-01](../../../../artefatos-tech-challenge-3/plano-implementacao/specs/SPEC-01-lambda-auth-cpf.md) §7; o RDS Proxy fica como plano B.
- **Single-AZ** (`multi_az = false`), sem failover automático. Aceito para ambiente de estudo.
- `skip_final_snapshot = true` e `deletion_protection = false`: um `terraform destroy` apaga os dados sem salvaguarda. Decisão deliberada, porque destruir o ambiente entre sessões é a estratégia de custo.
- Mover o subnet group para as subnets privadas ([ADR-007](./ADR-007-rede-privada-nat-instance.md)) implica modificação da instância, com janela de indisponibilidade.
