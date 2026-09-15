# Testes — oficina-api

Estratégia, organização e execução dos testes automatizados da aplicação.

## Resumo

| Indicador | Valor |
|---|---|
| Casos executados | 145 (132 métodos, parte deles parametrizada) |
| Cobertura de linhas (JaCoCo) | 91,0% |
| Cobertura de branches (JaCoCo) | 75,8% |
| Limites que falham o build | 85% de linhas e 70% de branches no projeto; 80% de instruções nos pacotes de domínio |
| Execução no CI | todo push, em qualquer branch |

## Execução

```bash
./mvnw test      # unitários e integração (requer Docker, usado pelo Testcontainers)
./mvnw verify    # testes + relatório de cobertura em target/site/jacoco/index.html
```

O teste de integração sobe um PostgreSQL 16 real com Testcontainers e aplica todas as migrations do Flyway, inclusive a `V9`, que cadastra o funcionário configurado em `application-test.yml`. Não há dependência de banco local nem de serviços externos: o SMTP é substituído por mock e o JWT é assinado no próprio teste com a chave de `src/test/resources/application-test.yml`.

## Organização

Os testes seguem as camadas da Clean Architecture, em `src/test/java/com/oficina/mecanica/`.

### Domínio (`domain/`) — 34 métodos

Java puro, sem Spring e sem mocks.

| Classe | Métodos | O que verifica |
|---|---|---|
| `OrdemServicoTest` | 15 | ciclo de status, transições inválidas, cálculo do orçamento, token de aprovação externa (geração com expiração, aprovação, reprovação, token inválido, expirado e já utilizado) |
| `CpfCnpjTest` | 6 | CPF e CNPJ válidos e inválidos, identificação do tipo e normalização da máscara |
| `PecaTest` | 5 | baixa e reposição de estoque, estoque insuficiente e estoque abaixo do mínimo |
| `PlacaTest` | 4 | formatos antigo e Mercosul, placa inválida e normalização |
| `ExceptionTest` | 4 | mensagens das exceções de recurso não encontrado, estoque insuficiente e transição inválida |

### Aplicação (`application/`) — 71 métodos

Casos de uso com repositórios e portas substituídos por Mockito.

| Classe | Métodos | O que verifica |
|---|---|---|
| `OrdemServicoUseCasesTest` | 22 | abertura, diagnóstico, inclusão de serviço com snapshot de preço, orçamento com notificação, aprovação interna e externa, conclusão, entrega, consulta e restrição do cliente às próprias ordens |
| `ClienteUseCaseTest` | 11 | cadastro com status ATIVO, CPF duplicado, buscas, atualização e remoção |
| `VeiculoUseCaseTest` | 12 | cadastro, cliente inexistente, placa duplicada, listagem por cliente, restrição do cliente aos próprios veículos, atualização e remoção |
| `PecaUseCaseTest` | 9 | cadastro, buscas, peças abaixo do estoque mínimo, reposição e remoção |
| `ServicoUseCaseTest` | 7 | cadastro, buscas, atualização e remoção de serviços |
| `RelatorioTempoMedioUseCaseTest` | 4 | tempo médio de execução, ausência de ordens e ordens sem datas |
| `ReprovarOrcamentoUseCaseTest` | 3 | estorno de estoque na reprovação interna e externa; token inválido não estorna |
| `AdicionarPecaAOSUseCaseTest` | 3 | baixa de estoque ao incluir peça, estoque insuficiente e OS inexistente |

### Infraestrutura (`infrastructure/`) — 15 métodos

| Classe | Métodos | O que verifica |
|---|---|---|
| `JwtServiceTest` | 8 | aceita tokens de cliente e de funcionário emitidos pela Lambda; recusa outra chave, outro emissor, token expirado, papel desconhecido e token malformado |
| `MicrometerMetricasOrdemServicoTest` | 5 | abertura de OS, contagem de transições e permanência no status, logs de abertura e transição com o ID da OS, falhas de transição e falhas de integração por motivo |
| `SmtpNotificacaoServiceTest` | 2 | envio do e-mail com os links de aprovação e reprovação; cliente inexistente |

### Integração (`infrastructure/OrdemServicoIntegrationTest`) — 12 métodos

Contexto Spring completo, MockMvc e PostgreSQL via Testcontainers.

| Cenário | O que verifica |
|---|---|
| Fluxo completo da OS | da abertura à entrega, com token de funcionário, persistência e todas as migrations |
| Estorno de estoque | reprovação devolve as peças ao estoque |
| Listagem | ordenação por prioridade de status e exclusão das OS finalizadas e entregues |
| Aprovação externa (POST JSON) | aprova com token válido e sem JWT |
| Aprovação externa inválida | token incorreto retorna 401 |
| Link de aprovação do e-mail | o GET exibe a confirmação sem alterar a OS; o POST do formulário aprova |
| Link de reprovação do e-mail | o GET exibe a confirmação sem alterar a OS; o POST do formulário reprova |
| Escape da página de confirmação | token com HTML é exibido escapado |
| Papéis | cliente recebe 403 nas rotas de gestão (peças, clientes e relatório) |
| Propriedade | cliente lista apenas as próprias ordens e veículos e recebe 404 para recursos de outro cliente |
| Seed de homologação | a migration `V9` cadastra o funcionário configurado |
| Contrato OpenAPI | a especificação gerada pela aplicação é igual a `docs/openapi.json` |

## Integração contínua

O job **Build & Test** do workflow `.github/workflows/ci-cd.yml` executa `mvn -B verify` a cada push, em qualquer branch. Todos os testes (domínio, aplicação, infraestrutura e integração) rodam nesse job, e o relatório JaCoCo é publicado como artefato da execução.

O job é status check obrigatório nas regras de proteção de `develop` e `main`: um Pull Request só pode ser mesclado com todos os testes aprovados e com os limites de cobertura atendidos. Na `main`, o SonarCloud repete o build com análise estática e cobertura.

## Contrato OpenAPI

`docs/openapi.json` é gerado pela própria aplicação e verificado a cada build. Se um endpoint mudar sem a atualização do arquivo, o teste falha. Para regenerar:

```bash
./mvnw test -Dtest=OrdemServicoIntegrationTest -DatualizarOpenApi=true
```

## Fora do escopo automatizado

| Tipo | Situação |
|---|---|
| Teste de carga | script k6 (`oficina-infra-k8s/cluster/k8s/loadtest/k6-script.js`) executado manualmente em homologação, para observar o HPA |
| Teste ponta a ponta no ambiente implantado | roteiro manual com `docs/demo.http`, executado na demonstração |
| Função de autenticação por CPF | testada no repositório `oficina-auth-lambda` (44 casos, incluindo PostgreSQL via Testcontainers) |
| Infraestrutura | validada nos repositórios de Terraform com `terraform fmt`, `terraform validate`, tflint e checkov |
