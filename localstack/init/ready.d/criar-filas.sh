#!/bin/bash
set -euo pipefail

criar_fila_com_dlq() {
  local nome=$1
  local url_dlq arn_dlq atributos
  url_dlq=$(awslocal sqs create-queue --queue-name "${nome}-dlq" \
    --attributes MessageRetentionPeriod=1209600 --query QueueUrl --output text)
  arn_dlq=$(awslocal sqs get-queue-attributes --queue-url "$url_dlq" \
    --attribute-names QueueArn --query Attributes.QueueArn --output text)
  atributos=$(printf '{"VisibilityTimeout":"30","MessageRetentionPeriod":"345600","ReceiveMessageWaitTimeSeconds":"20","RedrivePolicy":"{\\"deadLetterTargetArn\\":\\"%s\\",\\"maxReceiveCount\\":\\"5\\"}"}' "$arn_dlq")
  awslocal sqs create-queue --queue-name "$nome" --attributes "$atributos" >/dev/null
  echo "fila ${nome} criada com DLQ ${nome}-dlq"
}

for fila in os-eventos billing-comandos execution-comandos; do
  criar_fila_com_dlq "oficina-local-${fila}"
done
