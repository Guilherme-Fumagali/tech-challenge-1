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

    alt cliente não encontrado, INATIVO ou BLOQUEADO
        AF-->>C: 401 AUTENTICACAO_RECUSADA (resposta idêntica nos três casos)
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

**CPF sem cadastro e cadastro sem permissão recebem a mesma resposta.** O status, os cabeçalhos e o corpo são idênticos, e o motivo fica apenas no log. Assim, o endpoint não permite descobrir quais CPFs estão cadastrados.

**O `sub` é o UUID do cliente.** O CPF não é usado como identificador, para não ser registrado nos logs das requisições seguintes.

**A validação acontece duas vezes.** O authorizer valida o token no gateway, e o filtro da aplicação valida novamente. A autorização sobre cada recurso, como verificar se a OS pertence ao cliente, é feita pela aplicação.
