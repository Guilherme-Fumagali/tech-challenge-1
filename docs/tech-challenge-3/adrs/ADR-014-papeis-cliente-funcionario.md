# ADR-014 — Papéis de cliente e funcionário

- **Data:** 14/09/2026
- **Status:** Aceita

## Contexto

Após a transferência da autenticação para a Lambda ([ADR-003](./ADR-003-autenticacao-cpf-lambda.md)), o único papel emitido era `CLIENTE`, e toda rota `/api/**` exigia apenas um token válido. Com isso, qualquer cliente autenticado conseguia cadastrar e remover clientes, alterar o catálogo, movimentar estoque e avançar ordens de serviço de terceiros. Além disso, não havia forma de cadastrar o primeiro cliente: o cadastro exigia token, e o token só era emitido para CPF já cadastrado.

## Decisão

Separar os usuários em dois papéis, ambos emitidos pela mesma Lambda, com autenticação por CPF:

| Papel | Rota de autenticação | Origem | Acesso na aplicação |
|---|---|---|---|
| `FUNCIONARIO` | `POST /auth/funcionarios` | tabela `funcionarios` (migration `V8`) | todas as rotas `/api/**` |
| `CLIENTE` | `POST /auth` | tabela `clientes` | leitura das próprias ordens de serviço e dos próprios veículos |

- As regras de acesso por papel ficam no `SecurityConfig`. A verificação de propriedade fica nos casos de uso (`executarDoCliente`, `listarDoCliente`, `buscarPorIdDoCliente`), que tratam recurso de outro cliente como inexistente e retornam `404`.
- O primeiro funcionário de homologação é cadastrado pela migration Java `V9__SeedFuncionarioHomologacao`, com o CPF lido do placeholder `funcionario-seed-cpf`. O valor vem do secret `FUNCIONARIO_SEED_CPF` do `oficina-infra-k8s`, é aplicado somente no ambiente `staging` e não é versionado.
- A aprovação de orçamento pelo cliente continua pelo token enviado por e-mail, sem JWT.

## Alternativas consideradas

**Autenticação de funcionário com CPF e senha.** Mais adequada para um papel com acesso de gestão. Não adotada nesta entrega para manter um único mecanismo de autenticação na Lambda; registrada em DT-06.

**Papel único com verificação de propriedade em todas as rotas.** Manteria o catálogo e o estoque acessíveis a clientes, sem separar operações de gestão.

**Cadastro inicial por script SQL manual.** Exigiria acesso direto ao RDS, que está em subnet privada, e não seria reproduzível.

## Consequências

**Positivas**
- Operações de gestão ficam restritas a funcionários.
- O cliente acessa apenas os próprios dados, sem possibilidade de enumerar recursos de terceiros.
- O ambiente de homologação pode ser usado a partir do pipeline, sem intervenção manual no banco.

**Negativas**
- O funcionário também se autentica apenas pelo CPF; quem conhecer o CPF de um funcionário ativo obtém acesso de gestão (DT-06).
- A tabela `funcionarios` é consultada pela Lambda, o que acrescenta mais uma dependência de schema entre os dois repositórios.
