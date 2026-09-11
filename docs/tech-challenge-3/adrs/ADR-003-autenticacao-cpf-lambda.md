# ADR-003 — Emissão de JWT migra da aplicação para a Lambda

- **Data:** 04/09/2026
- **Status:** Aceita
- **RFC de origem:** RFC-003 (estratégia de autenticação)

## Contexto

O enunciado exige uma **function serverless** que valide o CPF do cliente, consulte sua existência e status na base, e **gere e devolva um token JWT** para consumo das APIs protegidas.

Hoje a aplicação faz isso sozinha: `AuthController` expõe `/api/auth/login`, `/refresh` e `/logout`; `JwtService` assina tokens HMAC; `RefreshTokenService` faz rotação de refresh tokens persistidos (migration `V2__refresh_tokens.sql`); `UserDetailsConfig` mantém **um único usuário `admin` em memória**, com senha vinda de `app.admin.password`. Não há autenticação por CPF, e `clientes` não tem coluna de status.

Se a Lambda emitir tokens e a aplicação também, passam a existir **dois emissores** para o mesmo espaço de recursos — dois lugares para revogar, dois lugares para rotacionar chave, duas semânticas de claims.

## Decisão

A **Lambda passa a ser o único emissor** de tokens para a superfície de API protegida. A aplicação vira **exclusivamente validadora**.

Concretamente:

1. `AuthController` é **removido**. Os endpoints `/api/auth/**` deixam de existir.
2. `JwtService` perde `gerarAccessToken`; mantém `isTokenValido` e `extrairUsername`, e ganha leitura de claims (`cpf`, `clienteId`, `role`).
3. `JwtAuthenticationFilter` permanece e passa a popular o `SecurityContext` a partir das claims emitidas pela Lambda.
4. `UserDetailsConfig` e o usuário `admin` em memória são **removidos** — a autorização passa a vir da claim `role`.
5. `RefreshTokenService` e a tabela `refresh_tokens` ficam **sem uso**. A tabela **não é derrubada** nesta fase.

## Alternativas consideradas

**Manter os dois emissores** — Lambda para clientes (CPF), aplicação para admin. Descartada: cria dois domínios de confiança sobre a mesma API, dobra os pontos de rotação de chave, e a superfície `/api/auth/**` continuaria pública atrás do gateway sem necessidade.

**Amazon Cognito User Pool como emissor** (Aula 05 de Serverless). Tecnicamente sólido — emite JWT, tem MFA, valida e-mail, e o API Gateway integra nativamente. Descartada porque o "usuário" da oficina **já existe na tabela `clientes`, identificado por CPF**; usar Cognito exigiria espelhar essa base num diretório externo e manter sincronismo, e a Lambda de validação de CPF continuaria necessária como trigger. O enunciado descreve literalmente uma function que "consulta a existência e o status do cliente na base de dados" — o caminho direto é mais fiel ao requisito.

**Derrubar `refresh_tokens` numa migration `V7`.** Descartada por ser irreversível sem backup e sem ganho funcional. Fica registrada como débito técnico.

## Consequências

**Positivas**
- Um único emissor, uma única chave, uma única semântica de claims.
- A aplicação deixa de ter responsabilidade de identidade — coerente com Clean Architecture, já que autenticação é preocupação de infraestrutura, não de domínio.
- Escala junto com o API Gateway, sem consumir pods do cluster para login.

**Negativas**
- **Não haverá refresh token no fluxo novo.** O cliente reautentica informando o CPF quando o token expira. Aceitável porque o fator de autenticação é o próprio CPF, e não há senha a digitar de novo.
- **Autenticação por CPF sozinho é fraca** — CPF é identificador, não segredo. É o que o enunciado pede, e fica registrado explicitamente: em produção real, isto exigiria segundo fator. Ver [SPEC-01](../../../../artefatos-tech-challenge-3/plano-implementacao/specs/SPEC-01-lambda-auth-cpf.md) §8.
- Testes de integração que hoje fazem login via `/api/auth/login` precisam ser reescritos para forjar um JWT com a chave de teste.
- A tabela `refresh_tokens` fica órfã no schema, com a migration `V2` preservada por imutabilidade do histórico Flyway.
