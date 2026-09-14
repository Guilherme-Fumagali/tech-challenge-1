# ADR-009 — JWT HMAC validado por Lambda authorizer

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

Definido que a Lambda emite o JWT ([ADR-003](./ADR-003-autenticacao-cpf-lambda.md)) e que o gateway é um HTTP API ([ADR-004](./ADR-004-api-gateway-http-api.md)), resta decidir como o token é assinado e onde é validado.

A restrição técnica determinante é que o JWT authorizer nativo do HTTP API é compatível com OIDC e exige `issuer`, `audience` e um endpoint JWKS para buscar chaves públicas. Ele não valida tokens HMAC, porque não há chave pública a publicar em um JWKS quando a assinatura é simétrica.

A aplicação já assina com HMAC: `JwtService` usa `Keys.hmacShaKeyFor(secret.getBytes(UTF_8))`, com o segredo vindo de `jwt.secret`, que atualmente o Terraform injeta no Secret do Kubernetes a partir da variável `jwt_secret`.

## Decisão

Manter JWT assinado com HMAC-SHA256, validado por um Lambda authorizer do tipo REQUEST no API Gateway.

- O segredo tem fonte única no Terraform e é gravado em SSM Parameter Store SecureString em `/oficina/<ambiente>/jwt-secret`.
- Três consumidores leem do mesmo local: a Lambda emissora, o Lambda authorizer e a aplicação, esta última via Secret do Kubernetes escrito pelo Terraform, como já ocorre hoje.
- Authorizer com cache de 300 s por token, para evitar uma invocação a cada requisição.
- **Defesa em profundidade:** o `JwtAuthenticationFilter` da aplicação continua validando o token. O gateway aplica o controle de acesso, e a aplicação atua como verificação adicional.

## Alternativas consideradas

**RS256 com JWKS e JWT authorizer nativo.** Tecnicamente superior: a chave privada fica apenas na Lambda emissora, a validação dispensa segredo compartilhado e o gateway valida sem invocar código. Descartada porque exigiria publicar e hospedar um endpoint JWKS, mais um recurso público a manter, e reescrever a assinatura na aplicação e na Lambda. Fica registrada como evolução natural desta decisão: se o número de validadores crescer, o segredo compartilhado se torna um passivo e esta ADR deve ser superada.

**Cognito User Pool como issuer, usando o JWT authorizer nativo.** Resolveria a validação sem código. Descartada em [ADR-003](./ADR-003-autenticacao-cpf-lambda.md) por razões de modelo de identidade.

**Sem authorizer: o gateway apenas faz proxy e a aplicação valida.** Menos componentes e menor latência. Descartada porque o enunciado exige proteger rotas sensíveis no gateway; permitir que requisições não autenticadas cheguem ao cluster contraria o objetivo O2.

**Segredo como variável de ambiente da Lambda, sem SSM.** Mais simples e sem chamada extra no cold start. Descartada porque haveria três cópias do segredo (env da Lambda emissora, env do authorizer e Secret do Kubernetes), com risco de divergência em uma rotação.

## Consequências

**Positivas**
- Mudança mínima na aplicação: `JwtService` mantém a lógica de parsing já existente e funcional.
- Fonte única do segredo, o que concentra a rotação em um único ponto.
- O cache de 300 s reduz significativamente as invocações do authorizer e o custo.
- Requisições sem token válido são rejeitadas antes de consumir recursos do cluster.

**Negativas**
- **Segredo simétrico compartilhado entre três componentes.** O vazamento em qualquer um deles permite forjar tokens válidos para todos. É o principal passivo de segurança desta decisão.
- **O cache de 300 s impede a revogação imediata.** Um token invalidado continua aceito pelo gateway por até 5 minutos. Considerado aceitável porque não há mecanismo de revogação no fluxo novo ([ADR-003](./ADR-003-autenticacao-cpf-lambda.md)).
- O Lambda authorizer adiciona latência e cold start próprio no primeiro acesso, além do cold start da função de autenticação.
- A rotação do segredo exige apply do Terraform e restart dos pods para recarregar o Secret.
