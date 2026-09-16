# ADR-013 — Pipelines com role própria e menor privilégio

- **Data:** 13/09/2026
- **Status:** Aceita

## Contexto

A Fase 2 autenticava o GitHub Actions na AWS por OIDC, sem chave estática, mas com uma única role com `AdministratorAccess`, cuja trust policy aceitava qualquer branch de qualquer um dos repositórios (`repo:<repo>:*`). Com quatro repositórios públicos, o controle de qualquer branch de qualquer um deles concederia administração total da conta.

Três pontos da implementação reforçaram a revisão:

- Repositórios criados recentemente recebem subject imutável no token OIDC (`repo:owner@id/repo@id:…`). A trust antiga não o reconhecia, e o primeiro apply do `oficina-infra-k8s` falhou por esse motivo.
- Senha do banco e segredo do JWT eram secrets do GitHub, repassados ao Terraform.
- Salvar o `tfplan` como artifact, prática comum para garantir que o apply use o mesmo plano revisado, exporia os valores das variáveis a qualquer usuário, já que os repositórios são públicos e o arquivo os armazena em texto puro.

## Decisão

- **Uma role por repositório**, cada uma com policy inline restrita aos recursos que aquele pipeline cria. A da aplicação apenas publica no ECR, lê dois parâmetros do SSM e edita o namespace `oficina`.
- **Trust restrita** a `develop` e `main` e aos environments `staging` e `prod`, cada environment limitado à própria branch. Branches de feature executam apenas validação de código e não obtêm credenciais.
- **Permissions boundary** obrigatório para toda role criada pelo pipeline da Lambda, o que impede que o próprio pipeline eleve a permissão da função.
- **Segredos gerados pelo Terraform** (`random_password`), um por ambiente, gravados diretamente em SSM SecureString. Não há secret de banco nem de JWT no GitHub.
- **Nenhum plano salvo como artifact.** O job de apply gera o plano novamente.
- Provisionamento dessa camada por `bootstrap/github-oidc.sh`, executado fora da pipeline, com as policies versionadas em `bootstrap/iam/`.

## Alternativas consideradas

**Manter `AdministratorAccess`, só restringindo a trust.** Restringe quem pode assumir a role, sem limitar as ações que ela permite. Um erro no pipeline, ou uma action comprometida, continuaria com poder total.

**Uma role por repositório e por ambiente, com condição por tag.** Separaria homologação de produção também no IAM. Rejeitada porque boa parte das ações de EC2, EKS e ELB não aceita condição por tag de forma consistente, o que tornaria a policy frágil. Registrada em DT-09.

**Criptografar o `tfplan` antes de publicar.** Preserva a garantia de aplicar o mesmo plano revisado, ao custo de mais um segredo para gerenciar. Rejeitada pela complexidade em relação ao risco, que se limita a mudanças manuais na AWS entre o plan e a aprovação.

**Rodar o bootstrap de IAM na própria pipeline.** O pipeline que define permissões precisaria de permissão para alterar a si mesmo, o que equivale a acesso de administrador.

## Consequências

**Positivas**
- O comprometimento de um repositório afeta apenas o que aquele pipeline pode alterar.
- Nenhum segredo de infraestrutura passa pelo GitHub ou aparece em log ou artifact.

**Negativas**
- Recursos novos exigem ajuste da policy. A falha aparece como `AccessDenied` explícito, com a ação e o recurso negados; o ajuste é validado no IAM Access Analyzer e simulado antes de ser aplicado.
- A camada de identidade depende de execução manual por um administrador.
- Sem plano salvo, o apply não garante aplicar o mesmo plano revisado caso a conta mude entre as duas etapas.
