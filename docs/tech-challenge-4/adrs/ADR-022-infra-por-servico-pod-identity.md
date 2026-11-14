# ADR-022 — Infraestrutura por serviço, identidade de pod e roteamento na borda

- **Data:** 29/10/2026
- **Status:** Aceita
- **Supera:** [ADR-001](../../tech-challenge-3/adrs/ADR-001-quatro-repositorios.md) quanto aos manifestos Kubernetes e ao banco fora do repositório da aplicação
- **Complementa:** [ADR-004](../../tech-challenge-3/adrs/ADR-004-api-gateway-http-api.md) (API Gateway em modo HTTP API)
- **Encerra:** DT-12 (manifestos aplicados por `kubectl` dentro do Terraform)

## Contexto

Na Fase 3, a infraestrutura estava em dois repositórios:

- `oficina-infra-k8s`: rede, cluster, gateway, e também os manifestos da aplicação, aplicados por um `null_resource`.
- `oficina-infra-db`: o RDS.

O pipeline da aplicação só trocava a imagem do Deployment. Com um serviço, o arranjo funcionava, mas gerava o débito DT-12: o Deployment tinha dois donos, e reaplicar a infraestrutura voltava a imagem para `:latest`.

O enunciado da Fase 4 pede que cada microsserviço tenha repositório, infraestrutura e banco próprios, e pipeline independente com deploy automatizado em Kubernetes. A aula 1 de Estrutura de Microsserviços associa a autonomia de cada serviço a "repositório dedicado e publicação de artefato independente e isolado".

Os serviços passam a usar SQS. Os pods só tinham a role do nó, compartilhada por todos e sem permissões de fila. Dar permissão de SQS à role do nó permitiria a qualquer pod ler qualquer fila.

## Decisão

- **Plataforma compartilhada** em `oficina-infra-k8s`:
  - VPC, NAT instance, EKS e add-on de Pod Identity.
  - NLB, API Gateway e rotas, um repositório ECR por serviço.
  - Parâmetros compartilhados, integração do New Relic, MailHog e metrics-server.
  - Não aplica mais nenhum manifesto de aplicação.
- **Cada serviço** tem no próprio repositório:
  - `k8s/`: Kustomize com base e overlays de homologação e produção (Deployment, Service, HPA, PDB, ServiceAccount, ConfigMap). Aplicado pelo pipeline do serviço com `kubectl apply -k`; o Secret é montado no deploy a partir do SSM.
  - `infra/`: Terraform com o próprio banco, a fila de entrada e a DLQ, a role de pod e a associação Pod Identity, e os parâmetros SSM que publica. Estado em `<serviço>/<env>/terraform.tfstate` no bucket existente; plan e apply por `workflow_dispatch` com environment.
- **Role de pod por serviço** (EKS Pod Identity), com *permissions boundary* `oficina-pod-boundary`. Só consome a própria fila e envia para as filas de destino.
- O pipeline de cada serviço tem role OIDC própria ([ADR-013](../../tech-challenge-3/adrs/ADR-013-identidade-das-pipelines.md)). Só cria roles de pod com o boundary e só lê o prefixo SSM do próprio serviço e os parâmetros compartilhados.
- O banco do OS Service sai do `oficina-infra-db` para `tech-challenge-1/infra/`. O repositório antigo permanece como registro da Fase 3.
- **Borda:** o mesmo NLB e o mesmo VPC Link atendem os três serviços, cada um com listener e NodePort próprios. O API Gateway, ponto único de entrada da aula 3 de Estrutura de Microsserviços, encaminha cada prefixo ao serviço dono:

| Serviço | Listener do NLB | NodePort | Prefixos no gateway |
|---|---|---|---|
| OS | 80 | 30080 | `/api/{proxy+}` (demais caminhos), `/actuator/health` |
| Billing | 8081 | 30081 | `/api/orcamentos`, `/api/pagamentos` |
| Execution | 8082 | 30082 | `/api/execucoes` |

  As rotas públicas (status da OS, decisão do orçamento por token, webhook do Mercado Pago e health) são declaradas uma a uma. Entre serviços, a chamada usa o DNS do Kubernetes, sem passar pelo gateway; `/internal/**` do OS não tem rota no gateway.

## Alternativas consideradas

**Manter a infraestrutura de dados centralizada (`oficina-infra-db` com os três bancos).** Preserva a separação de ciclo de vida entre aplicação e dados da ADR-001, mas deixa a infraestrutura de cada serviço fora do seu repositório e obriga um PR em outro repositório para qualquer mudança de fila ou banco. Descartada em favor da leitura literal do enunciado e da autonomia por serviço.

**IRSA (IAM Roles for Service Accounts).** Exige provedor OIDC do cluster e anotação na ServiceAccount, com a role amarrada ao issuer do cluster, que muda a cada provisionamento. A Pod Identity associa role e ServiceAccount por API, sem provedor OIDC. Descartada.

**Permissões de SQS na role do nó.** Simples, mas qualquer pod poderia consumir a fila de outro serviço. Descartada.

**Ingress controller ou AWS Load Balancer Controller na borda.** Concentrariam o roteamento no cluster, mas acrescentariam um componente com IAM próprio para operar. Descartados; um NLB por serviço também foi descartado pelo custo fixo de cada balanceador.

**Argo CD ou Flux (GitOps).** Separaria entrega e integração contínua, mas acrescentaria um componente ao cluster. Descartada para a fase; o deploy por `kubectl apply -k` no pipeline atende o requisito.

## Consequências

**Positivas**

- Um único dono por recurso: o serviço é dono do Deployment, do banco e da fila; a plataforma, do cluster e da borda.
- O deploy de um serviço não depende de reaplicar a infraestrutura compartilhada (DT-12 encerrado).
- Menor privilégio por pod e por pipeline.

**Negativas**

- A ordem de provisionamento ganha um passo por serviço, e o destroy da plataforma precisa conferir os bancos dos três serviços antes de remover a rede.
- As três roles de pipeline têm permissão de edição no mesmo namespace `oficina`; um pipeline consegue alterar recursos de outro serviço (débito registrado).
- Os NodePorts são um contrato fixo entre a plataforma e cada serviço, e o tráfego interno entre serviços é HTTP sem TLS.
- Criar as roles e o boundary exige executar o script de bootstrap com credencial administrativa, fora do pipeline.
