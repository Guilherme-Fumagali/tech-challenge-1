# C4 · Nível 2 — Contêineres

Atende o requisito de **diagrama de componentes com visão de nuvem, APIs, banco e monitoramento**.

```mermaid
C4Container
    title Sistema da Oficina — Contêineres (AWS us-east-1)

    Person(cliente, "Cliente", "Autentica pelo CPF")

    Container_Boundary(aws, "AWS") {
        Container(apigw, "API Gateway", "HTTP API", "Borda única. Rejeita requisição sem token válido antes de chegar ao cluster.")
        Container(authfn, "Lambda oficina-auth", "Java 21 · SAM", "Valida CPF, consulta cliente, emite JWT de 15 min.")
        Container(authorizer, "Lambda oficina-authorizer", "Java 21", "Valida a assinatura do JWT. Cache de 300 s.")
        Container(nlb, "NLB interno", "Network Load Balancer", "Alvo do VPC Link, aponta para o NodePort dos nós.")
        Container(app, "oficina-api", "Spring Boot 3.4 · Java 21", "Clean Architecture. Valida o JWT; não emite mais.")
        ContainerDb(rds, "RDS PostgreSQL 16", "db.t4g.micro", "Clientes, veículos, ordens, serviços e peças.")
        Container(ecr, "ECR", "Registry", "Imagens da aplicação.")
        Container(nat, "NAT instance", "t4g.nano", "Única saída para a internet das subnets privadas.")
    }

    System_Ext(newrelic, "New Relic", "Dashboards, alertas e monitor sintético")

    Rel(cliente, apigw, "POST /auth · ANY /api/**", "HTTPS")
    Rel(apigw, authfn, "Invoca", "AWS_PROXY")
    Rel(apigw, authorizer, "Autoriza rota protegida", "REQUEST authorizer")
    Rel(apigw, nlb, "Encaminha", "VPC Link")
    Rel(nlb, app, "NodePort 30080", "TCP")
    Rel(authfn, rds, "SELECT por cpf_cnpj", "JDBC")
    Rel(app, rds, "Leitura e escrita", "JDBC")
    Rel(app, ecr, "Imagem puxada via S3 gateway endpoint", "HTTPS")
    Rel(app, nat, "Telemetria", "HTTPS")
    Rel(nat, newrelic, "OTLP e agente", "HTTPS")
```

## Fronteiras de rede

| Zona | O que hospeda |
|---|---|
| Internet | API Gateway (gerenciado pela AWS) |
| Subnet **pública** | NAT instance apenas |
| Subnet **privada** | nós do EKS, RDS, NLB interno, Lambda de autenticação |

Nenhum nó ou banco tem IP público. A única saída é pela NAT instance, e a única entrada é pelo API Gateway.
