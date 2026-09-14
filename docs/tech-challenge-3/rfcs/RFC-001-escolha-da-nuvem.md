# RFC-001 — Escolha do provedor de nuvem

- **Autor:** Guilherme Fumagali Marques
- **Data:** 04/09/2026
- **Status:** Aceita → [ADR-002](../adrs/ADR-002-nuvem-aws.md)
- **Revisores:** Danilo Canato
- **Período de comentários:** 04/09 a 05/09/2026

> **Sobre o formato.** Nenhuma aula da fase aborda RFC; a disciplina de Documentação de Arquitetura cobre ADR, DAS, HLD/LLD, C4 e requisitos. Foi adotada a convenção usual: a RFC antecede a decisão e coleta comentários, e a ADR é escrita após a decisão e é imutável. A RFC registra a discussão e a ADR registra a conclusão. As seções reutilizam a estrutura da ADR, acrescentando revisores e período de comentários.

## Contexto

O enunciado da Fase 3 permite livre escolha de nuvem e exige do provedor cinco itens: API Gateway, function serverless, banco de dados gerenciado, cluster Kubernetes com escalabilidade e provisionamento por Terraform.

As disciplinas da fase utilizam provedores diferentes:

| Disciplina | Provedor demonstrado |
|---|---|
| API Gateway (aulas 2–3) | Azure API Management |
| API Gateway (aulas 4–6) | Kong, open source, agnóstico |
| Desenvolvimento Serverless (aulas 1–6) | AWS, integralmente |

A Fase 2 já entregou EKS e RDS em AWS, com OIDC para o GitHub Actions e backend de state em S3 + DynamoDB em funcionamento.

## Proposta

Adotar AWS, evoluindo a infraestrutura existente.

## Alternativas avaliadas

### Azure

Atende bem à disciplina de API Gateway: o APIM oferece portal do desenvolvedor, políticas, versionamento e cache prontos, e integra com Azure Functions e Azure AD.

**Contra:** descartaria todo o trabalho de infraestrutura da Fase 2 (cluster, banco, OIDC, state). Com peso ainda maior, a disciplina que sustenta o requisito central desta fase, a function de autenticação, é 100% AWS: Lambda, SAM, Cognito, VPC Link. A migração implicaria implementar o requisito mais importante sem o apoio do material coberto pelo curso.

### Multi-cloud (Lambda na AWS, APIM no Azure)

Cobriria as duas disciplinas de gateway.

**Contra:** duplica a superfície operacional e o custo, e adiciona latência e complexidade de rede entre nuvens, sem ganho para os requisitos. Descartada já na análise inicial.

### AWS

Reaproveita a infraestrutura existente e permite usar a disciplina de Serverless como guia direto de implementação.

**Contra:** o conteúdo de Azure APIM fica sem aplicação prática, e o SAM acopla a solução ao provedor, conforme alerta explícito da Aula 06.

## Comentários recebidos

**Danilo Canato:** *"Preocupação com o conteúdo de Azure ficar sem uso na entrega. Sugiro citar o APIM na comparação do documento, mostrando que a alternativa foi avaliada e por que foi descartada; assim a disciplina aparece na argumentação, mesmo sem implementação."*

**Resolução:** aceito. A seção 3 do documento de entrega compara AWS API Gateway, Azure APIM e Kong e apresenta a justificativa da escolha.

## Decisão

Foi adotada a AWS, conforme formalizado em [ADR-002](../adrs/ADR-002-nuvem-aws.md).

## Consequências

- Reaproveitamento de EKS, RDS, VPC, OIDC e backend de state já testados.
- `AmazonEC2ContainerRegistryReadOnly` já estava no role dos nós, o que tornou a migração para ECR praticamente sem custo.
- Acoplamento ao provedor, agravado pelo SAM.
- Custo recorrente efetivo, com o EKS control plane a US$ 73/mês como piso.
