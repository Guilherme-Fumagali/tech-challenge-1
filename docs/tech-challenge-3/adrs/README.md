# ADRs — Tech Challenge Fase 3

Registros de decisão arquitetural, no formato ensinado na Aula 05 de Documentação de Arquitetura de Soluções (a disciplina de Documentação de Arquitetura de Soluções).

## Índice

| ADR | Decisão | Status |
|---|---|---|
| [001](./ADR-001-quatro-repositorios.md) | Quatro repositórios, `oficina-api` preservado | Aceita |
| [002](./ADR-002-nuvem-aws.md) | AWS como provedor de nuvem | Aceita |
| [003](./ADR-003-autenticacao-cpf-lambda.md) | Emissão de JWT migra da aplicação para a Lambda | Aceita |
| [004](./ADR-004-api-gateway-http-api.md) | AWS API Gateway em modo HTTP API | Aceita |
| [005](./ADR-005-runtime-lambda-java-sam.md) | Lambda em Java 21 com AWS SAM | Aceita |
| [006](./ADR-006-apm-new-relic.md) | New Relic como plataforma de APM | Aceita |
| [007](./ADR-007-rede-privada-nat-instance.md) | Subnets privadas com NAT instance, não NAT Gateway nem VPC endpoints | Aceita |
| [008](./ADR-008-registry-ecr.md) | Registry de imagens migra de GHCR para ECR | Aceita |
| [009](./ADR-009-jwt-hmac-lambda-authorizer.md) | JWT HMAC validado por Lambda authorizer | Aceita |
| [010](./ADR-010-postgresql-rds.md) | PostgreSQL em RDS mantido | Aceita |
| [011](./ADR-011-hpa.md) | HPA por CPU e memória | Aceita (retroativa) |

## Processo

Conforme a Aula 05:

1. Quem identifica a necessidade escreve a ADR usando este formato e assume o papel de **dono**.
2. A ADR entra em **Proposta**. O dono conduz a revisão com o time.
3. Precisando de mudanças, **permanece em Proposta**. Rejeitada, **o motivo é registrado** — para não rediscutir depois.
4. Aprovada, passa para **Aceita** e **torna-se imutável**.
5. Mudança de rumo exige **nova ADR**, que ao ser aceita marca a anterior como **Superada**.

**A imutabilidade é o ponto que mais se erra.** ADR aceita não se edita — se supera. O histórico é o valor.

### Estados

`Proposta` · `Aceita` · `Rejeitada` · `Depreciada` · `Superada por ADR-NNN`

### Quando *não* abrir ADR

Decisão trivial, temporária ou reversível — bump de dependência, ajuste de configuração, renomear variável. Isso vai para changelog e histórico de commits. Registrar tudo sobrecarrega a documentação e afoga as decisões que importam.

## RFCs — a lacuna

O enunciado exige **RFCs** para decisões técnicas relevantes, mas **nenhuma aula da fase cobre RFC**. Extensão consciente, com esta convenção:

| | RFC | ADR |
|---|---|---|
| Momento | **antes** de decidir | **depois** de decidir |
| Propósito | propor direção e coletar comentários | registrar o que foi decidido e por quê |
| Estado final | vira uma ADR | imutável |

As três RFCs da fase (nuvem, banco, estratégia de autenticação) reusam as seções da ADR — Contexto, Proposta, Alternativas, Consequências — acrescentando **período de comentários** e **lista de revisores**. As ADRs 002, 003 e 010 são o resultado dessas RFCs; a RFC preserva a discussão, a ADR preserva a conclusão.
