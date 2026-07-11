#!/usr/bin/env bash
#
# Cria (ou remove) o backend remoto de state usado por ../environments/aws/backend.tf:
# um bucket S3 versionado + uma tabela DynamoDB de lock.
#
# POR QUE ISTO É UM SCRIPT E NÃO TERRAFORM
# ----------------------------------------
# O backend remoto não pode ser gerenciado pelo Terraform que o usa: no primeiro apply o
# bucket ainda não existe, então o state desse apply teria que ficar em disco — e um state
# local morre junto com o runner efêmero do CI (ou com a máquina de quem rodou). Na execução
# seguinte o Terraform "esqueceria" que o bucket existe, tentaria recriá-lo e falharia.
#
# A saída é não tratar o backend como recurso Terraform: ele vira um pré-requisito de
# infraestrutura, criado por este script idempotente. Script não tem state — pode rodar N
# vezes, converge sempre pro mesmo lugar, e não há nada pra perder. É o padrão usado na
# prática pra resolver esse ovo-e-galinha.
#
# Uso:
#   ./bootstrap.sh create    # cria o bucket + tabela (idempotente)
#   ./bootstrap.sh destroy   # remove os dois (só depois de destruir o ambiente aws!)
#
# Roda via .github/workflows/bootstrap-aws.yml (um clique no Actions) ou localmente com
# credenciais AWS já configuradas.

set -euo pipefail

AWS_REGION="${AWS_REGION:-us-east-1}"
LOCK_TABLE="${TFSTATE_LOCK_TABLE:-oficina-api-tfstate-lock}"
STATE_KEY="aws/terraform.tfstate"

log() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
die() { printf '\033[1;31mERRO:\033[0m %s\n' "$*" >&2; exit 1; }

command -v aws >/dev/null || die "AWS CLI não encontrado no PATH."

# O bucket vive no "account regional namespace": o nome termina em -<conta>-<região>-an e é
# reservado só pra esta conta AWS. Duas vantagens sobre o namespace global (default histórico):
# ninguém mais pode tomar o nome, e — mais importante — quando este bucket for deletado no fim
# do projeto, o nome NÃO volta pro pool global (onde outra conta poderia recriá-lo e passar a
# receber requisições destinadas ao bucket antigo). O nome é derivado, não fixo, pra o script
# continuar funcionando em qualquer conta/região.
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)" \
  || die "AWS CLI não está autenticado."
BUCKET="${TFSTATE_BUCKET:-oficina-api-tfstate-${ACCOUNT_ID}-${AWS_REGION}-an}"

bucket_exists() { aws s3api head-bucket --bucket "$BUCKET" >/dev/null 2>&1; }
table_exists()  { aws dynamodb describe-table --table-name "$LOCK_TABLE" --region "$AWS_REGION" >/dev/null 2>&1; }

create() {
  if bucket_exists; then
    log "Bucket s3://$BUCKET já existe — nada a criar."
  else
    log "Criando bucket s3://$BUCKET em $AWS_REGION..."
    # --bucket-namespace só é exigido no CreateBucket. Leitura/escrita (tudo que o backend do
    # Terraform faz depois) é idêntica à de um bucket global — nenhum ajuste necessário lá.
    local ns_flag=()
    case "$BUCKET" in
      *-an) ns_flag=(--bucket-namespace account-regional) ;;
    esac

    # us-east-1 é a única região que rejeita LocationConstraint (é o default da API).
    if [ "$AWS_REGION" = "us-east-1" ]; then
      aws s3api create-bucket --bucket "$BUCKET" --region "$AWS_REGION" "${ns_flag[@]}"
    else
      aws s3api create-bucket --bucket "$BUCKET" --region "$AWS_REGION" \
        --create-bucket-configuration "LocationConstraint=$AWS_REGION" "${ns_flag[@]}"
    fi
    aws s3api wait bucket-exists --bucket "$BUCKET"
  fi

  # Os três abaixo são idempotentes por natureza (PUT sobrescreve), então rodam sempre —
  # isso também conserta um bucket que tenha sido criado à mão sem essas proteções.
  log "Habilitando versionamento (permite recuperar um state corrompido)..."
  aws s3api put-bucket-versioning --bucket "$BUCKET" \
    --versioning-configuration Status=Enabled

  log "Habilitando criptografia em repouso (AES256)..."
  aws s3api put-bucket-encryption --bucket "$BUCKET" \
    --server-side-encryption-configuration \
    '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

  log "Bloqueando acesso público (o state contém endpoints e metadados da infra)..."
  aws s3api put-public-access-block --bucket "$BUCKET" \
    --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

  if table_exists; then
    log "Tabela DynamoDB $LOCK_TABLE já existe — nada a criar."
  else
    log "Criando tabela DynamoDB $LOCK_TABLE (lock de state, PAY_PER_REQUEST)..."
    aws dynamodb create-table \
      --table-name "$LOCK_TABLE" \
      --region "$AWS_REGION" \
      --billing-mode PAY_PER_REQUEST \
      --attribute-definitions AttributeName=LockID,AttributeType=S \
      --key-schema AttributeName=LockID,KeyType=HASH >/dev/null
    aws dynamodb wait table-exists --table-name "$LOCK_TABLE" --region "$AWS_REGION"
  fi

  log "Backend pronto. environments/aws/backend.tf já aponta pra bucket=$BUCKET, table=$LOCK_TABLE."
}

# Aborta se o state ainda tiver recursos: destruir o bucket agora deixaria EKS/RDS órfãos
# na conta (cobrando, e sem Terraform pra removê-los). A ordem correta é sempre:
# workflow "Destroy AWS" primeiro, este script depois.
assert_state_vazio() {
  if ! aws s3api head-object --bucket "$BUCKET" --key "$STATE_KEY" >/dev/null 2>&1; then
    log "Nenhum state em s3://$BUCKET/$STATE_KEY — nada de infra pra ficar órfão."
    return 0
  fi

  local tmp recursos
  tmp="$(mktemp)"
  aws s3api get-object --bucket "$BUCKET" --key "$STATE_KEY" "$tmp" >/dev/null
  recursos="$(jq '[.resources[]?] | length' "$tmp")"
  rm -f "$tmp"

  if [ "$recursos" -gt 0 ]; then
    die "O state ainda tem $recursos recurso(s) — ou seja, EKS/RDS provavelmente continuam de pé (cobrando).
     Rode o workflow 'Destroy AWS' ANTES deste. Se você tem certeza de que a conta já está limpa
     (conferido no console), force com: FORCE_DESTROY=1 ./bootstrap.sh destroy"
  fi
  log "State existe mas está vazio (0 recursos) — seguro destruir o backend."
}

destroy() {
  # Só o destroy depende de jq (ler o state, varrer as versões do bucket) — o create não usa.
  # Exigir jq lá em cima quebraria o create numa máquina que só tem o AWS CLI, sem motivo.
  command -v jq >/dev/null || die "jq não encontrado no PATH (necessário só para o destroy)."

  if [ "${FORCE_DESTROY:-0}" = "1" ]; then
    log "FORCE_DESTROY=1 — pulando a verificação de state órfão."
  else
    assert_state_vazio
  fi

  if bucket_exists; then
    log "Esvaziando s3://$BUCKET (todas as versões e delete markers)..."
    # Bucket versionado só é deletável se TODAS as versões forem removidas — não basta `rm`.
    while :; do
      local objetos qtd payload
      objetos="$(aws s3api list-object-versions --bucket "$BUCKET" --output json \
        | jq -c '[(.Versions // []), (.DeleteMarkers // [])] | flatten | map({Key, VersionId})')"
      qtd="$(echo "$objetos" | jq 'length')"
      [ "$qtd" -eq 0 ] && break

      payload="$(mktemp)"
      echo "$objetos" | jq -c '.[0:1000] | {Objects: ., Quiet: true}' > "$payload"
      aws s3api delete-objects --bucket "$BUCKET" --delete "file://$payload" >/dev/null
      rm -f "$payload"
    done

    log "Deletando bucket s3://$BUCKET..."
    aws s3api delete-bucket --bucket "$BUCKET" --region "$AWS_REGION"
  else
    log "Bucket s3://$BUCKET não existe — nada a deletar."
  fi

  if table_exists; then
    log "Deletando tabela DynamoDB $LOCK_TABLE..."
    aws dynamodb delete-table --table-name "$LOCK_TABLE" --region "$AWS_REGION" >/dev/null
    aws dynamodb wait table-not-exists --table-name "$LOCK_TABLE" --region "$AWS_REGION"
  else
    log "Tabela $LOCK_TABLE não existe — nada a deletar."
  fi

  log "Backend removido. A conta AWS não deve ter mais nenhum recurso deste projeto."
}

case "${1:-}" in
  create)  create ;;
  destroy) destroy ;;
  *)       die "Uso: $0 {create|destroy}" ;;
esac
