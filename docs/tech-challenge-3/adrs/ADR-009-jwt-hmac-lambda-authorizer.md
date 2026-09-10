# ADR-009 — JWT HMAC validado por Lambda authorizer

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

Definido que a Lambda emite o JWT ([ADR-003](./ADR-003-autenticacao-cpf-lambda.md)) e que o gateway é um HTTP API ([ADR-004](./ADR-004-api-gateway-http-api.md)), falta decidir **como o token é assinado e onde é validado**.

Restrição técnica decisiva: o **JWT authorizer nativo** do HTTP API é OIDC-compatível — exige `issuer`, `audience` e um endpoint **JWKS** para buscar chaves públicas. Ele **não valida tokens HMAC**, porque não existe chave pública a publicar num JWKS quando a assinatura é simétrica.

A aplicação já assina com HMAC: `JwtService` usa `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`, com o segredo vindo de `jwt.secret`, que hoje o Terraform injeta no Secret do Kubernetes a partir da variável `jwt_secret`.

## Decisão

Manter **JWT assinado com HMAC-SHA256**, validado por um **Lambda authorizer** do tipo REQUEST no API Gateway.

- Segredo é fonte única no Terraform, gravado em **SSM Parameter Store SecureString** em `/oficina/<ambiente>/jwt-secret`.
- Três consumidores leem do mesmo lugar: a Lambda emissora, o Lambda authorizer e a aplicação — esta última via Secret do Kubernetes escrito pelo Terraform, como já acontece hoje.
- Authorizer com **cache de 300 s** por token, para não invocar a cada requisição.
- **Defesa em profundidade:** o `JwtAuthenticationFilter` da aplicação continua validando o token. O gateway é o ponto de aplicação; a aplicação é a rede de segurança.

## Alternativas consideradas

**RS256 com JWKS e JWT authorizer nativo.** Tecnicamente superior: chave privada só na Lambda emissora, validação sem segredo compartilhado, e o gateway valida sem invocar código. Descartada porque exigiria **publicar e hospedar um endpoint JWKS** — mais um recurso público a manter — e reescrever a assinatura na aplicação e na Lambda. Fica registrada como a evolução natural desta decisão: se o número de validadores crescer, o segredo compartilhado vira passivo e esta ADR deve ser superada.

**Cognito User Pool como issuer, usando o JWT authorizer nativo.** Resolveria a validação sem código. Descartada em [ADR-003](./ADR-003-autenticacao-cpf-lambda.md) pelas razões de modelo de identidade.

**Sem authorizer: gateway só faz proxy, a aplicação valida.** Menos peças e latência menor. Descartada porque o enunciado exige **proteger rotas sensíveis no gateway**; deixar requisição não autenticada chegar ao cluster contraria o objetivo O2.

**Segredo como variável de ambiente da Lambda, sem SSM.** Mais simples e sem chamada extra no cold start. Descartada porque haveria **três cópias do segredo** — env da Lambda emissora, env do authorizer e Secret do Kubernetes — com risco de divergirem numa rotação.

## Consequências

**Positivas**
- Mudança mínima na aplicação: `JwtService` mantém a lógica de parsing que já existe e funciona.
- Fonte única do segredo, o que torna a rotação uma operação de um lugar só.
- Cache de 300 s reduz drasticamente as invocações do authorizer e o custo.
- Requisição sem token válido é rejeitada **antes** de consumir recurso do cluster.

**Negativas**
- **Segredo simétrico compartilhado entre três componentes.** Vazamento em qualquer um permite forjar tokens válidos para todos. É o principal passivo de segurança desta decisão.
- **Cache de 300 s significa que revogação não é imediata** — um token invalidado continua aceito pelo gateway por até 5 minutos. Aceitável porque não há mecanismo de revogação no fluxo novo ([ADR-003](./ADR-003-autenticacao-cpf-lambda.md)).
- Lambda authorizer adiciona latência e cold start próprio no primeiro acesso, além do cold start da função de autenticação.
- Rotação do segredo exige apply do Terraform **e** restart dos pods para recarregar o Secret.
