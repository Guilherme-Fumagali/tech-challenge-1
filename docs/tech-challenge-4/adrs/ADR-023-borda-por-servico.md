# ADR-023 — Roteamento na borda: listener e NodePort por serviço

- **Data:** 29/10/2026
- **Status:** Aceita
- **Complementa:** [ADR-004](../../tech-challenge-3/adrs/ADR-004-api-gateway-http-api.md) (API Gateway em modo HTTP API)

## Contexto

Na Fase 3, o caminho até a aplicação era API Gateway (HTTP API) → VPC Link → NLB interno → NodePort 30080 → Deployment `oficina-api`, com uma rota `ANY /api/{proxy+}` protegida pelo Lambda authorizer e algumas rotas públicas explícitas. Com três serviços, o gateway precisa encaminhar cada requisição ao serviço dono do recurso.

A aula 3 de Estrutura de Microsserviços apresenta o API Gateway como ponto único de entrada que desacopla clientes e serviços e pode implementar descoberta de serviço do lado do servidor. A aula 2 descreve a descoberta *server-side*: o cliente chama um roteador, que encaminha ao serviço.

## Decisão

O mesmo NLB e o mesmo VPC Link atendem os três serviços, cada um com listener, target group e NodePort fixos:

| Serviço | Listener do NLB | NodePort | Prefixos no gateway |
|---|---|---|---|
| OS | 80 | 30080 | `/api/{proxy+}` (demais caminhos), `/actuator/health` |
| Billing | 8081 | 30081 | `/api/orcamentos`, `/api/pagamentos` |
| Execution | 8082 | 30082 | `/api/execucoes` |

- Cada serviço tem integração `HTTP_PROXY` própria. O HTTP API escolhe a rota mais específica, então `/api/orcamentos/{proxy+}` prevalece sobre `/api/{proxy+}`.
- As rotas públicas são declaradas uma a uma, sem authorizer:
  - status da OS;
  - página e decisão do orçamento por token;
  - webhook do Mercado Pago;
  - health.
- As quatro rotas de autenticação (`/auth`, `/auth/confirmacao` e as equivalentes de funcionário) têm limite de 10 requisições/s com rajada de 20.
- A comunicação interna entre serviços (composição de API) usa o DNS do Kubernetes (`<serviço>.oficina.svc.cluster.local`). É descoberta do lado do servidor pelo `kube-proxy`, sem passar pelo gateway.

Endpoints internos (`/internal/**` do OS, usados pelo Lambda de autenticação) não têm rota no gateway; são alcançados só de dentro da VPC pelo listener do OS no NLB, com chave compartilhada no header.

## Alternativas consideradas

**Ingress controller no cluster (NGINX ou Gateway API) atrás de um único NodePort.** Concentraria o roteamento por caminho no cluster, mas acrescentaria um componente para operar. O controlador `ingress-nginx` deixou de receber correções, inclusive de segurança, em março de 2026, conforme anúncio da comunidade Kubernetes de novembro de 2025. Descartada.

**AWS Load Balancer Controller com ALB e regras por caminho.** Exigiria instalar o controlador com role IAM própria e trocar o NLB por um ALB gerenciado pelo cluster, o que acopla a borda ao ciclo de vida do controlador. Descartada.

**Um NLB por serviço.** Cada NLB custa cerca de US$ 16 por mês. Os listeners adicionais no mesmo NLB não têm custo fixo. Descartada.

**Service mesh (Istio).** Apresentado na aula 5 de Estrutura de Microsserviços II para mTLS, políticas e roteamento. Descartado pela capacidade de dois nós `t3.medium` e pela complexidade; a ausência de mTLS entre serviços fica registrada como limitação.

## Consequências

**Positivas**

- Nenhum componente novo no cluster; a borda continua inteiramente em Terraform.
- Cada serviço pode ser implantado sem alterar o gateway, desde que mantenha o NodePort.

**Negativas**

- Os NodePorts são um contrato fixo entre o repositório da plataforma e o de cada serviço.
- O tráfego interno entre serviços é HTTP sem TLS dentro do cluster.
- Rotas novas de um serviço ainda exigem PR no repositório da plataforma.
