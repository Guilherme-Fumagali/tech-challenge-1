# ADR-023 — Segundo fator na autenticação e desacoplamento do banco do OS

- **Data:** 29/10/2026
- **Status:** Aceita
- **Complementa:** [ADR-003](../../tech-challenge-3/adrs/ADR-003-autenticacao-cpf-lambda.md) (emissão do JWT pela Lambda)
- **Encerra:** DT-06 (autenticação por CPF sem segundo fator)

## Contexto

Na Fase 3, o cliente e o funcionário se autenticavam informando apenas o CPF. O débito DT-06 classificava o risco como alto: qualquer pessoa que conheça um CPF cadastrado obtém um token válido por 15 minutos. A banca registrou o ponto na avaliação da Fase 3.

O Lambda de autenticação consultava as tabelas `clientes` e `funcionarios` diretamente no banco da aplicação, com o usuário mestre do RDS. Na Fase 4 esse banco pertence ao OS Service, e o enunciado proíbe o acesso direto ao banco de outro serviço. Havia também um acoplamento de inicialização: o authorizer carregava os parâmetros do banco no *cold start* sem usá-los.

A aula 4 de Estrutura de Microsserviços recomenda tokens com expiração curta e padronizar a resposta de falha de autenticação para não dar pistas a um atacante.

## Decisão

1. **Segundo fator por código de uso único enviado por e-mail.**
   - `POST /auth` com o CPF responde `202` com um `desafioId`, sempre com o mesmo corpo, exista ou não o CPF.
   - Se o CPF estiver ativo, um código de 6 dígitos é enviado pelo Amazon SES ao e-mail cadastrado.
   - `POST /auth/confirmacao` com `desafioId` e código emite o JWT. Qualquer falha devolve o mesmo `401`.
   - Funcionários usam o mesmo fluxo em `/auth/funcionarios`.
2. **Desafio em DynamoDB.**
   - O código não é armazenado: guarda-se o HMAC-SHA256 de `desafioId:código` com um segredo do SSM.
   - Expira em 5 minutos (TTL da tabela, com verificação também no código).
   - Aceita no máximo 3 tentativas, controladas por atualização condicional.
   - É apagado ao ser usado.
3. **Identidade consultada pelo OS Service.** O Lambda chama `GET /internal/identidades/{clientes|funcionarios}/{cpf}` no OS, pelo listener interno do NLB, com o header `X-Chave-Interna`. A consulta tem timeout de 2 s; falha ou timeout respondem `503`, sem emitir token. O Lambda deixa de ter credenciais de banco.
4. O authorizer passa a carregar só o segredo do JWT.

## Alternativas consideradas

**TOTP (aplicativo autenticador).** Não depende de e-mail, mas exige cadastro prévio do segredo de cada cliente, com leitura de QR code, que a oficina não tem como fazer sem um frontend. Descartada.

**SMS.** Custo por mensagem e, no Amazon SNS/End User Messaging, registro de origem para envio no Brasil. Descartada.

**Amazon Cognito com MFA.** Substituiria o Lambda de autenticação e a emissão de token da Fase 3 por um provedor gerenciado; mudança de escopo maior que a fase comporta. Descartada.

**Segundo dado conhecido (CPF + placa ou número da OS).** São dois fatores de conhecimento; não acrescentam posse. Descartada.

**Read model de identidades no próprio Lambda (CQRS por eventos de cliente).** Eliminaria a chamada síncrona ao OS, mas exigiria consumidor de eventos, carga inicial e mais uma função no limite de 10 execuções simultâneas da conta (DT-11). Descartada; a consulta síncrona é o caso "REST síncrono quando necessário" do enunciado, já que a autenticação precisa do status atual.

## Consequências

**Positivas**

- Um CPF conhecido não basta para obter token: é preciso acesso ao e-mail cadastrado.
- O Lambda deixa de compartilhar o banco do OS; a dependência passa a ser um contrato HTTP versionado.
- Respostas idênticas para CPF existente, inexistente e inativo mantêm a proteção contra enumeração da Fase 3.

**Negativas**

- O SES começa em modo *sandbox*: só envia para endereços verificados, o que restringe a demonstração aos e-mails da equipe até a liberação da conta.
- O login passa a depender do OS Service, do SES e da NAT instance (SES é alcançado pela internet).
- O fluxo do cliente ganha um passo.
