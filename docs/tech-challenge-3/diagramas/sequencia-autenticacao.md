# Diagrama de sequência — Autenticação por CPF

Requisito literal do enunciado.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway
    participant AF as Lambda oficina-auth
    participant SSM as SSM Parameter Store
    participant DB as RDS PostgreSQL
    participant AZ as Lambda authorizer
    participant API as oficina-api (EKS)

    Note over C,API: Fase 1 — obtenção do token

    C->>GW: POST /auth { "cpf": "529.982.247-25" }
    GW->>AF: AWS_PROXY (sem authorizer)

    alt cold start do container
        AF->>SSM: GetParameter jwt-secret, db-*
        SSM-->>AF: valores (memoizados no container)
    end

    AF->>AF: Cpf.de() — normaliza e valida dígito verificador

    alt CPF malformado
        AF-->>C: 400 CPF_INVALIDO (sem tocar no banco)
    end

    AF->>DB: SELECT id, nome, status FROM clientes WHERE cpf_cnpj = ?
    DB-->>AF: linha ou vazio

    alt cliente não encontrado
        AF-->>C: 404 — "Não foi possível autenticar com o CPF informado."
    else status INATIVO ou BLOQUEADO
        AF-->>C: 403 — mesma mensagem do 404
    else ATIVO
        AF->>AF: assina JWT HS256 (sub=UUID, cpf, nome, role, exp=900s)
        AF-->>C: 200 { accessToken, tokenType, expiresIn }
    end

    Note over C,API: Fase 2 — consumo de rota protegida

    C->>GW: GET /api/ordens (Authorization: Bearer ...)

    alt token em cache (300s)
        GW->>GW: reusa decisão anterior
    else primeira vez
        GW->>AZ: REQUEST authorizer
        AZ->>AZ: verifica assinatura e issuer
        AZ-->>GW: { isAuthorized, context: { clienteId, role } }
    end

    alt não autorizado
        GW-->>C: 401 — requisição não chega ao cluster
    else autorizado
        GW->>API: VPC Link → NLB → NodePort
        API->>API: JwtAuthenticationFilter valida de novo (defesa em profundidade)
        API->>DB: consulta
        API-->>C: 200 + dados
    end
```

## Três detalhes que o diagrama torna explícitos

**404 e 403 devolvem a mesma mensagem.** O código específico existe para o log; o corpo é genérico de propósito, para que ninguém use o endpoint para descobrir quais CPFs existem na base.

**O `sub` é o UUID, não o CPF.** Assim o identificador não se espalha pelos logs de todo request downstream.

**A validação acontece duas vezes.** No gateway, pelo authorizer, que é o ponto de aplicação; e na aplicação, pelo filtro, que é a rede de segurança. O gateway autentica; quem decide se *este* cliente pode ver *esta* OS continua sendo a aplicação.
