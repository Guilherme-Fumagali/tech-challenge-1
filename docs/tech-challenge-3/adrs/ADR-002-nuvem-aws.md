# ADR-002 — AWS como provedor de nuvem

- **Data:** 04/09/2026
- **Status:** Aceita
- **RFC de origem:** RFC-001 (escolha da nuvem)

## Contexto

O enunciado permite livre escolha de nuvem e exige do provedor: API Gateway, function serverless, banco de dados gerenciado, cluster Kubernetes com escalabilidade e provisionamento por Terraform.

As disciplinas da fase cobrem provedores diferentes: a de API Gateway apresenta Azure API Management (aulas 2–3) e Kong open source (aulas 4–6); a de Serverless é inteiramente baseada em AWS (Lambda, API Gateway, ECS Fargate, Cognito, SAM). A Fase 2 já entregou EKS + RDS em AWS, com OIDC para o GitHub Actions e backend de state em S3 + DynamoDB em funcionamento.

## Decisão

Adoção da AWS, mantendo e evoluindo a infraestrutura da Fase 2.

## Alternativas consideradas

**Azure.** Atende bem à disciplina de API Gateway, com o APIM oferecendo portal do desenvolvedor, políticas e cache prontos. Descartada porque inutilizaria todo o trabalho de infraestrutura da Fase 2 e porque a disciplina de Serverless, que sustenta o requisito central de autenticação, é 100% AWS. A troca de nuvem implicaria implementar a function sem o apoio do material coberto pelo curso.

**Multi-cloud** (Lambda na AWS, APIM no Azure). Descartada já na análise inicial: duplica a superfície operacional, o custo e a complexidade de rede, sem ganho para os requisitos.

**Kong self-hosted no cluster EKS.** Mantém tudo na AWS e cobre a disciplina de API Gateway com a ferramenta open source. Continua sendo uma alternativa viável, mas foi descartada em [ADR-004](./ADR-004-api-gateway-http-api.md) por razões específicas do gateway.

## Consequências

**Positivas**
- Reaproveita EKS, RDS, VPC, OIDC e backend de state já provisionados e testados.
- A disciplina de Serverless serve como guia direto de implementação: Lambda, API Gateway, VPC Link, SAM.
- `AmazonEC2ContainerRegistryReadOnly` já está no role dos nós, o que torna a migração para ECR ([ADR-008](./ADR-008-registry-ecr.md)) praticamente sem custo.

**Negativas**
- O conteúdo de Azure APIM (aulas 2–3 de API Gateway) fica sem aplicação prática na entrega.
- Acoplamento ao provedor, agravado pelo SAM. A própria Aula 06 de Serverless alerta que o SAM "cria um grande acoplamento da sua solução com a AWS, e se houver necessidade de migrar de provedor, acarretará reconstruções".
- Custo recorrente efetivo, com o EKS control plane a US$ 73/mês como piso.
