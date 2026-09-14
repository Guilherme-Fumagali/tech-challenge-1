# RFC-003 — Estratégia de autenticação

- **Autor:** Guilherme Fumagali Marques
- **Data:** 04/09/2026
- **Status:** Aceita → [ADR-003](../adrs/ADR-003-autenticacao-cpf-lambda.md), [ADR-009](../adrs/ADR-009-jwt-hmac-lambda-authorizer.md)
- **Revisores:** Danilo Canato
- **Período de comentários:** 04/09 a 05/09/2026

## Contexto

O enunciado exige uma function serverless que valide o CPF do cliente, consulte sua existência e status na base e gere e devolva um token JWT para consumo das APIs protegidas, com as rotas sensíveis protegidas por um API Gateway.

Estado herdado da Fase 2:

- `AuthController` expõe `/api/auth/login`, `/refresh` e `/logout`.
- `JwtService` assina tokens HMAC-SHA256 com segredo de `jwt.secret`.
- `RefreshTokenService` faz rotação de refresh tokens persistidos (`V2__refresh_tokens.sql`).
- `UserDetailsConfig` mantém um único usuário `admin` em memória.
- Não há autenticação por CPF, e `clientes` não tem coluna de status.

Há uma restrição técnica que condiciona a proposta: o JWT authorizer nativo do HTTP API é compatível com OIDC e exige issuer, audience e um endpoint JWKS para buscar chaves públicas. Ele não valida HMAC, porque não há chave pública a publicar quando a assinatura é simétrica.

## Proposta

Três decisões acopladas:

1. **A Lambda passa a ser o único emissor.** A aplicação passa a atuar exclusivamente como validadora. `AuthController`, `UserDetailsConfig` e `RefreshTokenService` são removidos.
2. **JWT permanece HMAC-SHA256.** O segredo fica em SSM Parameter Store como fonte única para os três consumidores.
3. **Lambda authorizer.** Um authorizer do tipo REQUEST, com cache de 300 s, valida o token na borda, e o `JwtAuthenticationFilter` continua validando na aplicação, como defesa em profundidade.

## Alternativas avaliadas

### Manter dois emissores (Lambda para clientes, aplicação para admin)

Preservaria o acesso administrativo existente.

**Contra:** cria dois domínios de confiança sobre a mesma API, dobra os pontos de rotação de chave e mantém `/api/auth/**` público atrás do gateway sem necessidade. Com um único emissor, há apenas um ponto de rotação, uma semântica de claims e um ponto de auditoria.

### Amazon Cognito User Pool como emissor

Tecnicamente sólido: emite JWT, oferece MFA e validação de e-mail prontos, e o API Gateway integra nativamente com o authorizer OIDC, o que dispensaria escrever o authorizer.

**Contra:** o "usuário" da oficina já existe na tabela `clientes`, identificado por CPF. O uso de Cognito exigiria espelhar essa base em um diretório externo e manter a sincronização, e a Lambda de validação de CPF continuaria necessária como trigger. O enunciado descreve uma function que "consulta a existência e o status do cliente na base de dados", e o caminho direto é mais fiel ao requisito.

### RS256 com JWKS e authorizer nativo

Superior em segurança: a chave privada fica apenas na Lambda emissora, a validação dispensa segredo compartilhado e o gateway validaria sem invocar código.

**Contra:** exigiria publicar e hospedar um endpoint JWKS, mais um recurso público a manter, e reescrever a assinatura na aplicação e na Lambda. Fica registrada como evolução natural: se o número de validadores crescer, o segredo compartilhado se torna um passivo e a ADR-009 deve ser superada.

### Gateway só como proxy, validação apenas na aplicação

Menos componentes e menor latência.

**Contra:** o enunciado exige proteger rotas sensíveis no gateway. Permitir que requisições não autenticadas cheguem ao cluster contraria esse objetivo.

## Comentários recebidos

**Danilo Canato:** *"Autenticar só por CPF me incomoda. CPF é público: quem souber o CPF de um cliente consegue se autenticar como ele. Entendo que é o que o enunciado pede, mas essa limitação precisa ser declarada na entrega."*

**Resolução:** aceito integralmente. A limitação é declarada na seção 10 do documento de entrega, no README da Lambda e na SPEC-01 §8. Mitigações dentro do escopo: respostas 404 e 403 indistinguíveis (impede enumeração de CPFs), throttling de 10 req/s na rota `/auth`, token de 15 minutos sem refresh e CPF nunca registrado em log, apenas os 3 últimos dígitos.

**Danilo Canato:** *"Se removermos o refresh token, com que frequência o cliente precisará se reautenticar?"*

**Resolução:** a cada 15 minutos, informando o CPF novamente. Considerado aceitável porque o fator de autenticação é o próprio CPF e não há senha a redigitar. A tabela `refresh_tokens` permanece órfã no schema e é declarada como débito técnico, em vez de ser removida por uma migration irreversível.

## Decisão

Formalizada em [ADR-003](../adrs/ADR-003-autenticacao-cpf-lambda.md) e [ADR-009](../adrs/ADR-009-jwt-hmac-lambda-authorizer.md).

## Consequências

- Um único emissor, uma chave e uma semântica de claims.
- A aplicação deixa de ter responsabilidade de identidade, o que é coerente com Clean Architecture, já que autenticação é preocupação de infraestrutura.
- O segredo simétrico compartilhado entre três componentes é o principal passivo de segurança.
- Com cache de 300 s, a revogação não é imediata, e não há mecanismo de revogação no fluxo novo.
- Os testes de integração passam a forjar o JWT com a chave de teste, em vez de chamar um endpoint de login.
