# Testes — OS Service

Estratégia, organização e execução dos testes automatizados do OS Service (repositório `tech-challenge-1`).

## Resumo

| Indicador | Valor |
|---|---|
| Casos executados | 333 (domínio 129, aplicação 99, infraestrutura unitários 55, integração 43, cenários BDD 7) |
| Cobertura de linhas (JaCoCo) | 95,3% (2.078 linhas) |
| Cobertura de ramos (JaCoCo) | 85,1% (502 ramos) |
| Cobertura de instruções do domínio | 99,4% |
| Limites que falham o build | 85% de linhas e 70% de ramos no projeto; 80% de instruções nos pacotes de domínio |
| Execução no CI | todo push e todo pull request, job **Build & Test** |

Os números são da execução de `./mvnw -B verify` em 30/10/2026.

## Execução

```bash
./mvnw test      # todos os testes (requer Docker, usado pelo Testcontainers)
./mvnw verify    # testes + relatório de cobertura em target/site/jacoco/index.html e target/cucumber/relatorio.html
```

Os testes de integração e os cenários BDD compartilham um único contexto Spring e um único conjunto de contêineres, iniciados uma vez por execução em `IntegracaoBase`:

| Dependência | Implementação no teste |
|---|---|
| PostgreSQL 16 | Testcontainers `postgres:16-alpine`, com todas as migrations Flyway (V1 a V16) |
| SQS | Testcontainers `localstack/localstack:3`, com as três filas e as DLQs; a fila de eventos usa visibilidade de 2 s para acelerar os testes de nova tentativa |
| Billing e Execution (composição REST) | WireMock 3.9.2 |
| JWT | assinado no próprio teste com a chave de `src/test/resources/application-test.yml` |

## Organização

Os testes seguem as camadas da Clean Architecture, em `src/test/java/com/oficina/mecanica/`.

### Domínio (`domain/`) — 129 casos

Java puro, sem Spring e sem mocks.

| Classe | Casos | O que verifica |
|---|---|---|
| `saga/SagaOrdemServicoTest` | 60 | uma verificação por linha da tabela de transições da SPEC-08 §7.3, as quatro ordens de compensação (§7.4), eventos ignorados, eventos fora de ordem, rejeições sem compensação definida, prazos e reenvio (§7.5), reconstituição e log de passos |
| `StatusOSTest` | 19 | transições permitidas e proibidas, incluindo `AGUARDANDO_PAGAMENTO` |
| `OrdemServicoTest` | 16 | métodos nomeados pelo fato de negócio, histórico de transições com o evento, cancelamento com motivo, cálculo do orçamento e itens só em diagnóstico |
| `PlacaTest` | 13 | formatos antigo e Mercosul, placa inválida e normalização |
| `CpfCnpjTest` | 10 | CPF e CNPJ válidos e inválidos, identificação do tipo e normalização |
| `ExceptionTest` | 6 | mensagens das exceções de domínio |
| `PecaTest` | 5 | baixa e reposição de estoque, estoque insuficiente e estoque mínimo |

### Aplicação (`application/`) — 99 casos

Casos de uso com repositórios e portas substituídos por Mockito; a unidade de trabalho é substituída por `UnidadeDeTrabalhoImediata`, que conta as execuções.

| Classe | Casos | O que verifica |
|---|---|---|
| `OrdemServicoUseCasesTest` | 13 | abertura com início da saga, comando `IniciarDiagnostico` e prazo, entrega, consultas e restrição do cliente às próprias ordens |
| `ProcessarEventoSagaUseCaseTest` | 12 | duplicata descartada pela inbox, efeito na OS e na saga, comando com snapshot e prazo, estorno local, métricas, saga ou OS inexistente, evento fora de ordem e tipo desconhecido |
| `VeiculoUseCaseTest` | 12 | cadastro, placa duplicada, restrição do cliente aos próprios veículos, atualização e remoção |
| `ClienteUseCaseTest` | 11 | cadastro, CPF duplicado, buscas, atualização e remoção |
| `ConsultasSagaEIdentidadeTest` | 10 | consulta da saga, histórico por papel, identidade de cliente e de funcionário para a função de autenticação |
| `PecaUseCaseTest` | 9 | cadastro, buscas, estoque mínimo, reposição e remoção |
| `ServicoUseCaseTest` | 7 | cadastro, buscas, atualização e remoção |
| `MontadorDadosComandoTest` | 6 | dados de `IniciarDiagnostico`, `GerarOrcamento` (preços com duas casas), `IniciarReparo` e comandos de compensação |
| `CompensacaoEstornoEstoqueTest` | 5 | devolução das peças, idempotência (OS já cancelada), transição inválida sem tocar no estoque |
| `RelatorioTempoMedioUseCaseTest` | 4 | tempo médio de execução |
| `VerificarPrazosSagaUseCaseTest` | 4 | reenvio com nova mensagem, falha após o limite de reenvios e saga sem comando registrado |
| `AdicionarPecaAOSUseCaseTest` | 3 | baixa de estoque, estoque insuficiente e OS inexistente |
| `ConsultarAcompanhamentoUseCaseTest` | 3 | composição das três fontes, fonte que falha vira nula e indisponível, cliente de outra OS |

### Infraestrutura — unitários (`infrastructure/`) — 55 casos

| Classe | Casos | O que verifica |
|---|---|---|
| `messaging/TradutorEventosTest` | 19 | instante do fato de cada evento do contrato, etapa, comando rejeitado com motivo e código, campos ausentes |
| `security/JwtServiceTest` | 8 | tokens de cliente e de funcionário; outra chave, outro emissor, expirado, papel desconhecido e malformado |
| `messaging/ConsumidorEventosOsTest` | 5 | entrega ao caso de uso, contagem por resultado, tipo desconhecido, erro propagado para nova tentativa e envelope inválido |
| `observability/MicrometerMetricasOrdemServicoTest` | 5 | métricas de abertura e transição da OS |
| `security/ChaveInternaFilterTest` | 4 | chave correta, ausente, diferente e não configurada nas rotas `/internal/**` |
| `web/GlobalExceptionHandlerTest` | 4 | 409 por versão concorrente e por integridade, 500 genérico e status preservado de erros do protocolo |
| `ConfiguracaoDePerfisTest` | 2 | segredo JWT sem valor padrão fora dos perfis `local` e `test`; SQS do perfil `local` |
| `messaging/CabecalhosNewRelicTest` | 2 | cabeçalhos de rastreamento distribuído |
| `messaging/PropriedadesMensageriaTest` | 2 | chave lógica e nome real das filas |
| `messaging/RelayOutboxTest` | 2 | marcação de publicada e falha registrada sem interromper o lote |
| `observability/MicrometerMetricasSagaTest` | 2 | nomes e atributos da SPEC-14 §2 |

### Integração — 43 casos

Contexto Spring completo, MockMvc, PostgreSQL e LocalStack.

| Classe | Casos | O que verifica |
|---|---|---|
| `OrdemServicoIntegrationTest` | 9 | inclusão de itens só em diagnóstico com baixa de estoque, entrega, ordenação da listagem, papéis, propriedade, seed da V9 e contrato `docs/openapi.json` |
| `MensageriaIntegrationTest` | 8 | relay publica o envelope com os atributos `tipo` e `versao` e marca a linha; evento aplicado com registro na inbox; duplicata descartada; evento fora de ordem e corpo inválido indo para a DLQ após cinco recebimentos; gauge da DLQ; atributo `JavaType` ignorado; reprovação com devolução do estoque |
| `AcompanhamentoIntegrationTest` | 7 | composição com WireMock: sucesso com repasse do `Authorization`, limite de 2 s, circuito aberto sem chamada ao participante, abertura do circuito após falhas, 404 sem indisponibilidade, 4xx sem abrir o circuito, restrição do cliente |
| `SegurancaEErrosIntegrationTest` | 6 | 401 e 403 em JSON, corpo malformado, 405/404/415 preservados, 409 por integridade e status público com três campos |
| `IdentidadeInternaIntegrationTest` | 5 | identidade de cliente e de funcionário com a chave interna, e-mail da migration V15, 401 sem chave, 404 sem repetir o documento e 400 para documento inválido |
| `UnidadeDeTrabalhoIntegrationTest` | 4 | falha depois da baixa de estoque desfaz a baixa, gravação conjunta, versão desatualizada recusada e versão sincronizada em duas gravações |
| `SagaOrdemServicoIntegrationTest` | 2 | abertura com `IniciarDiagnostico` na fila, consultas da saga e do histórico por papel, reenvio por prazo vencido e falha após o limite |
| `ActuatorIntegrationTest` | 2 | sondas de saúde e commit em execução em `/actuator/info` |

### Cenários BDD (`src/test/resources/features/saga_ordem_servico.feature`) — 7 cenários

Cucumber 7.20.1 com Gherkin em português (`# language: pt`), executado pela JUnit Platform Suite (`bdd/ExecutarCenariosBddTest`) dentro do mesmo `./mvnw verify`. Os passos estão em `bdd/PassosOrdemServico`. As ações de pessoas no OS Service usam a API; Billing e Execution são simulados por passos que publicam os eventos na fila de entrada do OS e conferem os comandos nas filas de comandos. O texto dos cenários não cita filas, para que os mesmos arquivos possam ser executados contra o ambiente integrado.

| Cenário | Resultado verificado |
|---|---|
| Fluxo completo até a entrega do veículo | sequência de status de `RECEBIDA` a `ENTREGUE`, total do orçamento, saga `CONCLUIDA`, estoque baixado |
| Reprovação do orçamento | `CancelarExecucao`, OS `CANCELADA`, saga `COMPENSADA`, estoque devolvido |
| Pagamento recusado | `CancelarOrcamento` e depois `CancelarExecucao`, estoque devolvido |
| Falha na execução com estorno | `EstornarPagamento` e depois `CancelarOrcamento`, estoque devolvido |
| Diagnóstico concluído sem itens | `ReabrirDiagnostico`, nenhum pedido de orçamento; com itens, novo diagnóstico gera o pedido |
| Aviso repetido de pagamento aprovado | um único pedido de reparo |
| Reenvio por prazo vencido | novo pedido de diagnóstico com outra identificação e saga retomada |

O relatório HTML fica em `target/cucumber/relatorio.html` e é publicado como artefato `cucumber-report` no CI.

## Integração contínua

O job **Build & Test** do workflow `.github/workflows/ci-cd.yml` executa `./mvnw -B verify` em todo push e em todo pull request. Todos os testes (domínio, aplicação, infraestrutura, integração e BDD) rodam nesse job; o resumo de cobertura vai para o sumário da execução e os relatórios JaCoCo e Cucumber são publicados como artefatos. O job é verificação obrigatória nas regras de proteção de `develop` e `main`, ao lado de **SonarCloud** e **Format & validate (infra)**.

O `maven-surefire-plugin` está fixado na versão 3.5.6: a 3.5.3, gerenciada pelo Spring Boot 3.4.7, executa os cenários da JUnit Platform Suite mas não os contabiliza e não falha o build quando um cenário falha.

## Contrato OpenAPI

`docs/openapi.json` é gerado pela aplicação e conferido a cada build. Se um endpoint mudar sem a atualização do arquivo, o teste falha. Para regenerar:

```bash
./mvnw test -Dtest=OrdemServicoIntegrationTest -DatualizarOpenApi=true
```

O contrato das mensagens está em `docs/asyncapi.yaml`; os exemplos seguem o envelope verificado em `MensageriaIntegrationTest`.

## Fora do escopo automatizado

| Tipo | Situação |
|---|---|
| Saga contra os três serviços implantados | roteiro manual com `docs/postman/oficina-fase4.postman_collection.json` no ambiente de homologação |
| Teste de carga | não executado nesta fase |
| Função de autenticação, Billing e Execution | testados nos respectivos repositórios |
| Infraestrutura (`infra/`) | `terraform fmt`, `terraform validate`, tflint e checkov no workflow `infra.yml` |
