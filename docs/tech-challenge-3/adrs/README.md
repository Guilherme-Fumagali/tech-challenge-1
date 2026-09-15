# ADRs — Tech Challenge Fase 3

Registros de decisão arquitetural no formato apresentado na Aula 05 da disciplina de Documentação de Arquitetura de Soluções.

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
| [011](./ADR-011-hpa.md) | HPA por CPU e memória | Superada por ADR-015 |
| [012](./ADR-012-ambientes-segregados.md) | Homologação e produção em infraestrutura segregada | Aceita |
| [013](./ADR-013-identidade-das-pipelines.md) | Pipelines com role própria e menor privilégio | Aceita |
| [014](./ADR-014-papeis-cliente-funcionario.md) | Papéis de cliente e funcionário | Aceita |
| [015](./ADR-015-hpa-apenas-cpu.md) | HPA apenas por CPU | Aceita |

## Processo

Conforme a Aula 05:

1. Quem identifica a necessidade escreve a ADR neste formato e assume o papel de dono.
2. A ADR entra no estado Proposta, e o dono conduz a revisão com o time.
3. Se forem necessárias mudanças, a ADR permanece em Proposta. Se for rejeitada, o motivo é registrado, para evitar que o tema seja rediscutido.
4. Se aprovada, passa para Aceita e torna-se imutável.
5. Uma mudança de rumo exige nova ADR, que, ao ser aceita, marca a anterior como Superada.

Após aceita, a ADR não é editada; mudanças de decisão são registradas em uma nova ADR, que marca a anterior como superada, preservando o histórico.

### Estados

`Proposta` · `Aceita` · `Rejeitada` · `Depreciada` · `Superada por ADR-NNN`

### Quando não abrir ADR

Decisões triviais, temporárias ou reversíveis, como atualização de dependência, ajuste de configuração ou renomeação de variável, não geram ADR. Esses casos ficam registrados no changelog e no histórico de commits. Registrar todas as decisões sobrecarregaria a documentação e dificultaria a identificação das decisões relevantes.

## RFCs

O enunciado exige RFCs para decisões técnicas relevantes, mas nenhuma aula da fase aborda RFC. Por isso, o material foi complementado com a seguinte convenção:

| | RFC | ADR |
|---|---|---|
| Momento | antes de decidir | depois de decidir |
| Propósito | propor direção e coletar comentários | registrar o que foi decidido e por quê |
| Estado final | vira uma ADR | imutável |

As três RFCs da fase (nuvem, banco, estratégia de autenticação) reutilizam as seções da ADR (Contexto, Proposta, Alternativas, Consequências), acrescentando período de comentários e lista de revisores. As ADRs 002, 003 e 010 resultam dessas RFCs; a RFC registra a discussão e a ADR registra a conclusão.
