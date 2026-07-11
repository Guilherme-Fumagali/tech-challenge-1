#!/usr/bin/env bash
#
# Dá ao GitHub Actions permissão de agir na sua conta AWS, via OIDC (sem access key estática
# guardada em secret, que vaza e nunca é rotacionada). Cria:
#   1. o Identity Provider OIDC do GitHub na sua conta;
#   2. uma IAM role que SÓ este repositório consegue assumir;
#   3. imprime o ARN da role, pra você colar em Settings → Secrets → AWS_ROLE_ARN.
#
# ESTE É O ÚNICO PASSO QUE PRECISA RODAR NA SUA MÁQUINA. E não por falta de vontade: é
# impossível automatizar em pipeline por definição — pra criar a credencial que o pipeline
# usa, o pipeline precisaria já ter uma credencial. Alguém, uma vez, tem que rodar isso com
# as próprias credenciais de admin. Daí pra frente tudo (backend, EKS, RDS, deploy, destroy)
# é um clique no Actions.
#
# Pré-requisitos: AWS CLI autenticado com um usuário/role com permissão de IAM
# (`aws configure` ou `aws sso login`).
#
# Uso:
#   ./github-oidc.sh                       # usa o repo default abaixo
#   GITHUB_REPO=owner/repo ./github-oidc.sh

set -euo pipefail

GITHUB_REPO="${GITHUB_REPO:-Guilherme-Fumagali/tech-challenge-1}"
ROLE_NAME="${ROLE_NAME:-oficina-api-github-actions}"
PROVIDER_HOST="token.actions.githubusercontent.com"

log() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
die() { printf '\033[1;31mERRO:\033[0m %s\n' "$*" >&2; exit 1; }

command -v aws >/dev/null || die "AWS CLI não encontrado no PATH."

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)" \
  || die "AWS CLI não está autenticado. Rode 'aws configure' antes."
PROVIDER_ARN="arn:aws:iam::${ACCOUNT_ID}:oidc-provider/${PROVIDER_HOST}"

log "Conta AWS: $ACCOUNT_ID | Repo autorizado: $GITHUB_REPO"

# ── 1. Identity Provider OIDC ────────────────────────────────────────────────
if aws iam get-open-id-connect-provider --open-id-connect-provider-arn "$PROVIDER_ARN" >/dev/null 2>&1; then
  log "Identity Provider OIDC já existe."
else
  log "Criando Identity Provider OIDC do GitHub..."
  # A AWS valida o certificado do GitHub pela CA raiz desde 2023 — o thumbprint virou
  # vestigial, mas a API ainda exige o campo. Este é o valor publicado pelo GitHub.
  aws iam create-open-id-connect-provider \
    --url "https://${PROVIDER_HOST}" \
    --client-id-list "sts.amazonaws.com" \
    --thumbprint-list "6938fd4d98bab03faadb97b34396831e3780aea1" >/dev/null
fi

# ── 2. IAM role assumível só por este repo ───────────────────────────────────
# JSON inline em vez de `file://$(mktemp)`: no Windows/git-bash o aws.exe é um binário
# nativo e não entende um path POSIX como /tmp/tmp.XXXX. Inline funciona nos dois mundos.
TRUST_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "${PROVIDER_ARN}" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "${PROVIDER_HOST}:aud": "sts.amazonaws.com" },
      "StringLike":   { "${PROVIDER_HOST}:sub": "repo:${GITHUB_REPO}:*" }
    }
  }]
}
EOF
)

if aws iam get-role --role-name "$ROLE_NAME" >/dev/null 2>&1; then
  log "Role $ROLE_NAME já existe — atualizando a trust policy."
  aws iam update-assume-role-policy --role-name "$ROLE_NAME" \
    --policy-document "$TRUST_POLICY"
else
  log "Criando role $ROLE_NAME..."
  aws iam create-role --role-name "$ROLE_NAME" \
    --description "Deploy do oficina-api via GitHub Actions (Tech Challenge Fase 2)" \
    --assume-role-policy-document "$TRUST_POLICY" >/dev/null
fi

# AdministratorAccess é deliberado, não preguiça: o Terraform aqui cria VPC, IGW, IAM roles,
# EKS e RDS — uma policy "mínima" pra isso tem dezenas de actions e qualquer uma faltando
# quebra o apply no meio, deixando infra pela metade (cobrando). Num projeto acadêmico de
# uso pontual, o risco de escopo largo é menor que o de um apply parcial. Em produção real,
# isto seria uma policy dedicada e revisada.
log "Anexando AdministratorAccess (ver comentário no script sobre esse trade-off)..."
aws iam attach-role-policy --role-name "$ROLE_NAME" \
  --policy-arn "arn:aws:iam::aws:policy/AdministratorAccess"

ROLE_ARN="$(aws iam get-role --role-name "$ROLE_NAME" --query 'Role.Arn' --output text)"

cat <<EOF

┌──────────────────────────────────────────────────────────────────────────────
│ Pronto. Agora, no GitHub:
│
│   Settings → Secrets and variables → Actions → New repository secret
│     Nome:  AWS_ROLE_ARN
│     Valor: ${ROLE_ARN}
│
│ E crie também os secrets: TF_VAR_DB_PASSWORD, TF_VAR_JWT_SECRET (>=32 chars),
│ TF_VAR_ADMIN_PASSWORD.
│
│ Depois disso, tudo roda pelo Actions — começando por "Bootstrap AWS" (action: create).
└──────────────────────────────────────────────────────────────────────────────
EOF
