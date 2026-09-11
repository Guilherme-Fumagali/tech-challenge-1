# ADR-004 — AWS API Gateway em modo HTTP API

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

O enunciado exige um API Gateway para controle e roteamento, protegendo rotas sensíveis. Escolhida a AWS ([ADR-002](./ADR-002-nuvem-aws.md)), restam duas famílias: o AWS API Gateway (com três modos) ou um gateway self-hosted no cluster, como Kong ou Traefik.

A Aula 04 de Serverless detalha os três modos do AWS API Gateway:

| Modo | Perfil |
|---|---|
| **HTTP API** | mais recente e leve, baixa latência, **menor custo**; ideal para serverless e microsserviços |
| **REST API** | recursos avançados: chaves de API, throttling por cliente, validação de request, **integração com WAF**, endpoints privados |
| **WebSocket API** | comunicação bidirecional persistente |

A mesma aula descreve o padrão de integração com serviço privado: **API Gateway → VPC Link → load balancer interno → container**.

## Decisão

**AWS API Gateway em modo HTTP API**, com duas integrações:

- `POST /auth` → integração direta com a Lambda de autenticação.
- `ANY /api/{proxy+}` → **VPC Link** → NLB interno → Service do EKS, protegido por Lambda authorizer ([ADR-009](./ADR-009-jwt-hmac-lambda-authorizer.md)).

## Alternativas consideradas

**REST API.** Traz throttling por cliente, validação de request e integração com WAF. Descartada porque nenhum desses recursos é exigido e o custo por chamada é maior. Se o rate limiting por cliente virar requisito, esta ADR é superada.

**Kong no cluster EKS** (aulas 4–6 de API Gateway). Cobriria a disciplina com a ferramenta open source estudada e o plugin JWT resolveria a validação com Consumers. Descartada por três razões: (a) rodaria **dentro** do cluster que deveria proteger, deixando o Service exposto se alguém acessar o cluster por outro caminho; (b) exigiria Postgres próprio ou modo DB-less, mais peças para manter; (c) a integração nativa entre AWS API Gateway e Lambda é direta, enquanto no Kong a Lambda precisaria de um Function URL público ou de um plugin específico.

**Traefik como Ingress Controller.** Mesma objeção do Kong quanto a rodar dentro do cluster, e menos recursos de gateway de API.

**NLB público direto, sem gateway.** Descartada: viola o requisito explícito.

## Consequências

**Positivas**
- Custo próximo de zero na escala do projeto — 1 milhão de chamadas/mês no free tier.
- Integração nativa com a Lambda, sem expor Function URL.
- Menor latência que REST API, o que importa porque o authorizer já adiciona um salto.
- TLS gerenciado pela AWS, sem certificado para renovar.

**Negativas**
- **Sem WAF.** HTTP API não integra com AWS WAF; a proteção contra abuso fica limitada ao throttling padrão da conta.
- **Sem chave de API nem throttling por cliente.** Um CPF válido pode chamar à vontade dentro do limite global.
- Conteúdo de Azure APIM e Kong das aulas fica sem aplicação prática.
- O VPC Link é cobrado à parte e a própria Aula 04 alerta para as taxas de transferência de dados dentro e fora da VPC.
