# Evidências da validação em homologação

Capturas e registros coletados em 14/11/2026 no ambiente de homologação (`staging`), criado pelos pipelines a partir da branch `develop` e usado com as credenciais de teste do Mercado Pago. Os horários dos registros estão em UTC.

## Qualidade e entrega

| Arquivo | Conteúdo |
|---|---|
| `01-github-ruleset-main.jpg` | regra de proteção das branches `main` e `develop`: pull request, uma aprovação e checks obrigatórios |
| `02-sonarcloud-projetos-main.jpg` | SonarCloud dos três serviços na `main`: quality gate aprovado e cobertura |
| `14-github-pr-release-aprovado.jpg` | pull request para a `main` aprovado pelo revisor, com os checks e o deploy |
| `03-actions-terraform-cluster.jpg` | pipeline de infraestrutura que cria a rede, o EKS e o API Gateway |
| `07-actions-cicd-os-deploy.jpg` | pipeline do OS Service: testes, cobertura, SonarCloud, imagem e deploy no EKS |
| `15-actions-cicd-execution-deploy.jpg` | pipeline do Execution Service |
| `16-actions-cicd-billing-deploy.jpg` | pipeline do Billing Service |

## Infraestrutura

| Arquivo | Conteúdo |
|---|---|
| `cluster-kubectl.txt` | nós, deployments, pods, HPA, PDB e services do namespace `oficina` |
| `gateway-rotas.txt` | rotas do API Gateway e quais exigem o authorizer |
| `04-aws-rds-postgres.jpg` | PostgreSQL do OS Service e do Billing Service, em instâncias separadas |
| `06-aws-documentdb.jpg` | DocumentDB do Execution Service |
| `05-aws-sqs-filas.jpg` | filas de comandos e de eventos, cada uma com a sua DLQ |

## Fluxo da ordem de serviço

| Arquivo | Conteúdo |
|---|---|
| `fluxo-feliz.log` | requisições e respostas do fluxo completo: abertura, diagnóstico, orçamento, aprovação, pagamento, reparo e entrega; saga `CONCLUIDA` |
| `fluxo-compensacao.log` | orçamento reprovado: execução cancelada, peças devolvidas ao estoque, ordem `CANCELADA` e saga `COMPENSADA` |
| `08-mailhog-email-orcamento.jpg` | e-mail do orçamento com os links de aprovação e reprovação |
| `11-mercado-pago-webhooks-200.jpg` | notificações do Mercado Pago entregues ao Billing Service com resposta 200 |

Nos registros, o JWT e o token do link de decisão aparecem mascarados.

## Monitoramento

| Arquivo | Conteúdo |
|---|---|
| `12-newrelic-apm-servicos.jpg` | os três serviços no APM do New Relic |
| `09-newrelic-trace-distribuido.jpg` | um trace atravessando Execution, OS e Billing pelas filas |
| `13-newrelic-log-correlacao.jpg` | log estruturado com `os.id`, `saga.id` e `mensagem.id`, ligado ao trace |
| `17-newrelic-dashboard-saga.jpg` | dashboard da saga |
