# Débitos Técnicos Conhecidos — Fase 4

Registro versionado das simplificações conscientes da Fase 4. O `documento_entrega.docx` (seção 13) referencia este arquivo. Os débitos da Fase 3 estão em [`../tech-challenge-3/debitos-tecnicos.md`](../tech-challenge-3/debitos-tecnicos.md); a situação de cada um após esta fase está na tabela abaixo.

## Situação dos débitos anteriores

| Débito | Situação na Fase 4 |
|---|---|
| DT-01 · E-mail de orçamento sem detalhamento de itens | **encerrado**: o e-mail do Billing Service lista os itens, o total e a validade |
| DT-02 · Segredo JWT simétrico compartilhado | **aberto e agravado**: o segredo passou a ser lido por cinco componentes (emissor, authorizer e três serviços) |
| DT-03 · Revogação de token não é imediata | aberto, sem alteração |
| DT-04 · NAT instance é ponto único de falha | **aberto e agravado**: a NAT também é o caminho para SQS, SES e Mercado Pago (ver DT-19) |
| DT-05 · Homologação e produção compartilham cluster e banco | encerrado na Fase 3 |
| DT-06 · Autenticação por CPF sem segundo fator | **encerrado** pela ADR-024 |
| DT-07 · Tabela `refresh_tokens` órfã | **encerrado**: tabela e entidade removidas |
| DT-08 · Validação de CPF duplicada entre repositórios | aberto, sem alteração (OS Service e Lambda) |
| DT-09 · Ambientes na mesma conta AWS | aberto; ver também DT-16 |
| DT-10 · Produção preparada e nunca provisionada | **aberto, mantido por custo** |
| DT-11 · Concorrência reservada da Lambda depende de cota | aberto; nenhuma função nova foi criada |
| DT-12 · Manifests aplicados por `kubectl` dentro do Terraform | **encerrado** pela ADR-022 |

---

## DT-10 · Produção preparada e nunca provisionada

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

Os três serviços têm overlay `prod`, environment `prod` com revisor obrigatório e pipeline que publica em produção a partir de `main`. A plataforma aceita `ambiente = prod`. Nenhum recurso de produção foi criado.

### Causa raiz

Com três bancos e o DocumentDB, cada hora de ambiente ligado custa ~US$ 0,34 (`custos.md`). Manter produção em paralelo à homologação dobraria o custo das sessões de trabalho e de gravação.

### Melhoria planejada

Provisionar produção pela mesma esteira (dispatch dos workflows de infraestrutura em `main`) quando houver orçamento, validando primeiro o ciclo completo de apply e destroy.

### Justificativa de escopo

A banca registrou o ponto na Fase 3. A decisão de não provisionar foi tomada pelo custo; a configuração de produção é a mesma de homologação, com outra branch e outro environment.

### Impacto do débito

O caminho até produção (aprovação no environment, overlay `prod`) nunca foi exercitado em execução real.

---

## DT-13 · Código transversal replicado em três repositórios

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

Segurança JWT, tratamento de erros, outbox, inbox, propagação de trace, métricas de mensageria e unidade de trabalho existem em cópias independentes no OS, no Billing e no Execution.

### Causa raiz

Escolha da ADR-016: uma biblioteca compartilhada criaria dependência de versão entre os serviços e exigiria um repositório e um registro de pacotes.

### Melhoria planejada

Extrair um *chassis* (`oficina-chassis`) publicado no GitHub Packages, começando por segurança e mensageria, que são idênticos nos três serviços.

### Justificativa de escopo

Três serviços sob o mesmo time; a duplicação é visível e testada em cada repositório.

### Impacto do débito

Correção de segurança ou ajuste no envelope de mensagem precisa ser aplicado três vezes; divergência entre as cópias não é detectada automaticamente.

---

## DT-14 · Isolamento entre pods e bancos apenas por credencial

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

Os security groups dos bancos liberam acesso a partir do security group do cluster, compartilhado por todos os pods. Um pod de um serviço alcança, pela rede, o banco de outro; o que impede o acesso é não ter a credencial.

### Causa raiz

Security groups por pod exigem tipos de instância com suporte a *trunk ENI* e configuração adicional do VPC CNI; NetworkPolicy não distingue bancos que usam a mesma porta.

### Melhoria planejada

Habilitar security groups para pods (ou NetworkPolicy com o agente do VPC CNI) e restringir cada banco ao SG do respectivo serviço.

### Justificativa de escopo

As credenciais de cada banco só existem no Secret do próprio serviço e no prefixo SSM que só o pipeline do serviço lê.

### Impacto do débito

O vazamento de uma credencial de banco não é contido pela rede.

---

## DT-15 · Bancos em instância única

**Status:** aberto · **Prioridade:** baixa · **Risco:** médio

### Situação atual

Os dois RDS são Single-AZ e o DocumentDB tem uma instância, sem réplica.

### Causa raiz

Custo: Multi-AZ dobra o valor da instância; uma réplica do DocumentDB acrescenta ~US$ 0,075/h.

### Melhoria planejada

Multi-AZ nos RDS e uma réplica do DocumentDB em produção.

### Justificativa de escopo

Ambiente de estudo destruído ao fim de cada sessão.

### Impacto do débito

Falha da instância interrompe o serviço dono do banco até a recuperação. As mensagens destinadas a ele permanecem na fila e são processadas na volta.

---

## DT-16 · Roles de pipeline com edição no mesmo namespace

**Status:** aberto · **Prioridade:** baixa · **Risco:** baixo

### Situação atual

As três roles de pipeline têm `AmazonEKSEditPolicy` no namespace `oficina`. O pipeline de um serviço consegue alterar o Deployment de outro.

### Causa raiz

Um namespace para os três serviços; a política de acesso do EKS tem escopo de namespace, não de recurso.

### Melhoria planejada

Um namespace por serviço, com access entry restrita a ele.

### Justificativa de escopo

Os três pipelines estão sob o mesmo controle e só aplicam o diretório `k8s/` do próprio repositório.

### Impacto do débito

Um pipeline comprometido afeta os três serviços.

---

## DT-17 · Saga em FALHA exige intervenção manual

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

Após três reenvios sem resposta, ou quando um participante rejeita uma compensação, a saga vai para `FALHA`, gera alerta e para. Não há ferramenta para retomá-la.

### Causa raiz

Os casos que levam a `FALHA` dependem de análise: participante fora por longo período, comando inválido, inconsistência de dados.

### Melhoria planejada

Endpoint administrativo para reenviar o último comando ou executar uma compensação específica, com registro no log da saga.

### Justificativa de escopo

O runbook `saga-falha.md` descreve o diagnóstico e o reprocessamento pela DLQ (redrive) e pelo banco.

### Impacto do débito

Uma OS pode ficar parada até ação de um operador.

---

## DT-18 · Tráfego entre serviços sem TLS

**Status:** aberto · **Prioridade:** baixa · **Risco:** baixo

### Situação atual

A composição do acompanhamento e a consulta de identidade do Lambda usam HTTP dentro da VPC. As filas SQS usam TLS por padrão.

### Causa raiz

Não há service mesh nem certificados internos; o mTLS da aula 5 de Estrutura de Microsserviços II exigiria Istio ou equivalente nos dois nós.

### Melhoria planejada

Service mesh com mTLS automático ou certificados emitidos por ACM Private CA para os endpoints internos.

### Justificativa de escopo

O tráfego não sai da VPC; o JWT e a chave interna continuam sendo exigidos.

### Impacto do débito

Um atacante com acesso à rede da VPC poderia ler tokens em trânsito.

---

## DT-19 · Saída para SQS, SES e Mercado Pago depende da NAT instance

**Status:** aberto · **Prioridade:** média · **Risco:** médio

### Situação atual

Pods e Lambda alcançam SQS, SES e Mercado Pago pela NAT instance (DT-04). Só DynamoDB e S3 têm gateway endpoint.

### Causa raiz

Interface endpoints custam ~US$ 0,01/h por AZ cada (ADR-007).

### Melhoria planejada

Interface endpoint de SQS nas duas AZs quando o volume justificar.

### Justificativa de escopo

Com a NAT fora, a saga para mas não perde mensagens: o outbox acumula e o relay publica na volta.

### Impacto do débito

Queda da NAT pausa o avanço das sagas, o login (SES) e as cobranças.

---

## DT-20 · SES em modo sandbox

**Status:** aberto · **Prioridade:** média · **Risco:** baixo

### Situação atual

O SES só envia para endereços verificados. O código do segundo fator chega apenas aos e-mails da equipe usados na demonstração.

### Causa raiz

A saída do sandbox exige solicitação à AWS com descrição do caso de uso.

### Melhoria planejada

Solicitar acesso de produção ao SES antes de usar o login com clientes reais.

### Justificativa de escopo

Suficiente para homologação e demonstração.

### Impacto do débito

Cliente com e-mail não verificado não consegue autenticar enquanto a conta estiver em sandbox.

---

## DT-21 · Outbox publicado e inbox sem expurgo

**Status:** aberto · **Prioridade:** baixa · **Risco:** baixo

### Situação atual

As mensagens publicadas permanecem na tabela (ou coleção) `outbox` com a data de publicação, e a tabela `mensagens_processadas` do OS e do Billing cresce indefinidamente. No Execution, a coleção `mensagensProcessadas` tem TTL de 14 dias; o `outbox` não.

### Causa raiz

O expurgo não fazia parte dos contratos da fase; os registros servem de trilha durante os ensaios.

### Melhoria planejada

Job diário que remove mensagens publicadas e registros de inbox com mais de 14 dias, mesmo prazo do TTL do Execution.

### Justificativa de escopo

Volume de demonstração de dezenas de mensagens por ordem de serviço, com o ambiente destruído ao fim de cada sessão.

### Impacto do débito

Em operação contínua, as tabelas crescem e a consulta do relay passa a depender só do índice parcial de pendentes.
