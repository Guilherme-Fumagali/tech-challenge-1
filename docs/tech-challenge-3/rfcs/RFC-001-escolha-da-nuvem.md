# RFC-001 — Escolha do provedor de nuvem

- **Autor:** Guilherme Fumagali Marques
- **Data:** 04/09/2026
- **Status:** Aceita → [ADR-002](../adrs/ADR-002-nuvem-aws.md)
- **Revisores:** Danilo Canato
- **Período de comentários:** 04/09 a 05/09/2026

> **Sobre o formato.** Nenhuma aula da fase cobre RFC — a disciplina de Documentação de Arquitetura cobre ADR, DAS, HLD/LLD, C4 e requisitos. Adotamos a convenção usual: a **RFC vem antes da decisão e coleta comentários**; a **ADR vem depois e é imutável**. A RFC preserva a discussão, a ADR preserva a conclusão. As seções reusam a estrutura da ADR, acrescentando revisores e período de comentários.

## Contexto

O enunciado da Fase 3 dá **livre escolha de nuvem** e exige dela cinco coisas: API Gateway, function serverless, banco de dados gerenciado, cluster Kubernetes com escalabilidade e provisionamento por Terraform.

As disciplinas da fase apontam para provedores diferentes:

| Disciplina | Provedor demonstrado |
|---|---|
| API Gateway (aulas 2–3) | Azure API Management |
| API Gateway (aulas 4–6) | Kong, open source, agnóstico |
| Desenvolvimento Serverless (aulas 1–6) | **AWS**, integralmente |

A Fase 2 já entregou EKS e RDS em AWS, com OIDC para o GitHub Actions e backend de state em S3 + DynamoDB funcionando.

## Proposta

Adotar **AWS**, evoluindo a infraestrutura existente.

## Alternativas avaliadas

### Azure

Cobre bem a disciplina de API Gateway: o APIM entrega portal do desenvolvedor, políticas, versionamento e cache prontos, e integra com Azure Functions e Azure AD.

**Contra:** descartaria todo o trabalho de infraestrutura da Fase 2 — cluster, banco, OIDC, state. E, mais grave, a disciplina que sustenta o requisito central desta fase (a function de autenticação) é **100% AWS**: Lambda, SAM, Cognito, VPC Link. Migrar significaria implementar o requisito mais importante contra material que o curso não cobriu.

### Multi-cloud (Lambda na AWS, APIM no Azure)

Cobriria as duas disciplinas de gateway.

**Contra:** duplica superfície operacional, custo, e adiciona latência e complexidade de rede entre nuvens, sem nenhum ganho para os requisitos. Descartada de imediato.

### AWS

Reaproveita o que existe e transforma a disciplina de Serverless em manual direto de implementação.

**Contra:** o conteúdo de Azure APIM fica sem aplicação prática, e o SAM acopla a solução ao provedor — alerta explícito da Aula 06.

## Comentários recebidos

**Danilo Canato:** *"Preocupação com o conteúdo de Azure ficar sem uso na entrega. Sugiro citar o APIM na comparação do documento, mostrando que a alternativa foi avaliada e por que foi descartada — assim a disciplina aparece na argumentação, mesmo sem implementação."*

**Resolução:** aceito. A seção 3 do documento de entrega compara AWS API Gateway, Azure APIM e Kong, com a justificativa da escolha.

## Decisão

**AWS.** Formalizada em [ADR-002](../adrs/ADR-002-nuvem-aws.md).

## Consequências

- Reaproveita EKS, RDS, VPC, OIDC e backend de state já testados.
- `AmazonEC2ContainerRegistryReadOnly` já estava no role dos nós, o que tornou a migração para ECR quase gratuita.
- Acoplamento ao provedor, agravado pelo SAM.
- Custo real recorrente, com o EKS control plane a US$ 73/mês como piso.
