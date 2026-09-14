# ADR-012 — Homologação e produção em infraestrutura segregada

- **Data:** 13/09/2026
- **Status:** Aceita

## Contexto

O enunciado exige deploy automático das branches de homologação e produção. O plano original resolvia isso em um único ambiente: namespaces `oficina-staging` e `oficina` no mesmo cluster EKS e databases distintos na mesma instância RDS. A solução estava registrada como DT-05, com a ressalva de que não havia isolamento de falha efetivo.

Duplicar a infraestrutura foi inicialmente descartado pelo custo do EKS control plane. Contudo, um ambiente declarado em código, com state, pipeline e proteção próprios, não gera custo enquanto não é provisionado. Com base nisso, a equipe decidiu segregar os ambientes e realizar a demonstração em homologação.

## Decisão

Dois ambientes completos, sem nenhum recurso compartilhado:

| | Homologação | Produção |
|---|---|---|
| Branch | `develop` | `main` |
| GitHub Environment | `staging`, sem aprovação | `prod`, com revisor |
| Nomes | `oficina-api-staging-*` | `oficina-api-prod-*` |
| Rede | VPC própria | VPC própria |
| State | `infra-k8s/staging/<stack>`, `infra-db/staging` | `infra-k8s/prod/<stack>`, `infra-db/prod` |
| Contrato | `/oficina/staging/…` | `/oficina/prod/…` |
| Segredos | gerados pelo Terraform, por ambiente | gerados pelo Terraform, por ambiente |

Cada repositório segue o mesmo fluxo nos dois ambientes:

| Repositório | Push na branch do ambiente | Disparo manual |
|---|---|---|
| `oficina-infra-k8s` | validate, plan e apply da stack `base` (ECR + SSM) | stack `cluster` |
| `oficina-infra-db` | validate e plan | apply do RDS |
| `oficina-auth-lambda` | build, testes e deploy | — |
| `tech-challenge-1` | build, testes, imagem e deploy | — |

Os jobs de deploy e plan verificam previamente se o ambiente existe. Sem cluster ou sem ECR, o job é pulado com aviso, sem falhar e sem solicitar aprovação.

Homologação é o ambiente em execução e usado na demonstração. Produção fica preparada e protegida, mas não provisionada.

## Alternativas consideradas

**Namespace e database no mesmo cluster e instância.** Corresponde à solução anterior. É barata, mas um teste de carga em homologação disputa CPU com produção, e uma migration incorreta afeta a mesma instância. Rejeitada por não isolar falhas.

**Uma conta AWS por ambiente, via AWS Organizations.** Oferece o isolamento mais forte, com IAM, cotas e faturamento separados. Rejeitada nesta fase pelo esforço de operar múltiplas contas em uma conta de estudo. Registrada como evolução em DT-09.

**Só produção, com a ausência de homologação justificada.** Descumpre texto explícito do enunciado.

## Consequências

**Positivas**
- Isolamento completo: rede, dados, segredos e state de um ambiente não alcançam o outro.
- O custo de operação é igual ao de um único ambiente, porque apenas homologação é provisionada.
- Ativar produção consiste em disparar os mesmos workflows na `main` e aprovar, sem implementação adicional.

**Negativas**
- **Produção nunca foi exercitada de ponta a ponta.** O primeiro provisionamento pode revelar permissão faltante ou diferença de configuração. O risco é mitigado por se tratar do mesmo código de homologação e está registrado em DT-10.
- Os dois ambientes estão na mesma conta, e cada pipeline usa uma única role para ambos (DT-09).
- Todo merge na `main` do `oficina-infra-k8s` deixa um apply da `base` de produção aguardando aprovação.
- Supera a solução descrita em DT-05, que passa a constar como quitado.
