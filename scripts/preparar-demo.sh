#!/usr/bin/env bash
set -euo pipefail

AMBIENTE=${AMBIENTE:-staging}
CPF_FUNCIONARIO=${CPF_FUNCIONARIO:?informe CPF_FUNCIONARIO}
CPF_CLIENTE=${CPF_CLIENTE:-529.982.247-25}
EMAIL_CLIENTE=${EMAIL_CLIENTE:-cliente@example.com}
PLACA=${PLACA:-ABC1D23}
TOTAL_OS=${TOTAL_OS:-3}
PAUSA=${PAUSA:-0.3}

for comando in curl jq; do
  command -v "$comando" >/dev/null || { echo "requer $comando" >&2; exit 1; }
done

if [[ -z ${API_URL:-} ]]; then
  API_URL=$(aws ssm get-parameter --name "/oficina/${AMBIENTE}/api-gateway-url" --query Parameter.Value --output text)
fi
API_URL=${API_URL%/}

RESPOSTA=$(mktemp)
trap 'rm -f "$RESPOSTA"' EXIT
TOKEN=""

chamar() {
  local metodo=$1 caminho=$2 corpo=${3:-}
  local argumentos=(-sS -o "$RESPOSTA" -w '%{http_code}' -X "$metodo" "${API_URL}${caminho}")
  [[ -n $TOKEN ]] && argumentos+=(-H "Authorization: Bearer ${TOKEN}")
  [[ -n $corpo ]] && argumentos+=(-H 'Content-Type: application/json' -d "$corpo")
  local status
  status=$(curl "${argumentos[@]}")
  if [[ $status != 2* ]]; then
    echo "${metodo} ${caminho} retornou ${status}: $(cat "$RESPOSTA")" >&2
    exit 1
  fi
  cat "$RESPOSTA"
}

autenticar_funcionario() {
  TOKEN=""
  local desafio codigo
  desafio=$(chamar POST /auth/funcionarios "$(jq -nc --arg cpf "$CPF_FUNCIONARIO" '{cpf: $cpf}')" | jq -r .desafioId)
  if [[ -z $desafio || $desafio == null ]]; then
    echo "a função de autenticação não devolveu desafio" >&2
    exit 1
  fi
  read -r -p "Código enviado ao e-mail do funcionário: " codigo </dev/tty
  chamar POST /auth/funcionarios/confirmacao \
    "$(jq -nc --arg desafio "$desafio" --arg codigo "$codigo" '{desafioId: $desafio, codigo: $codigo}')" \
    | jq -r .accessToken
}

buscar_ou_criar() {
  local listagem=$1 filtro=$2 criacao=$3 corpo=$4
  local id
  id=$(chamar GET "$listagem" | jq -r "$filtro | .id" | head -n1)
  if [[ -z $id ]]; then
    id=$(chamar POST "$criacao" "$corpo" | jq -r .id)
  fi
  echo "$id"
}

echo "API: ${API_URL}"
TOKEN=$(autenticar_funcionario)

CPF_DIGITOS=${CPF_CLIENTE//[^0-9]/}
CLIENTE_ID=$(buscar_ou_criar /api/clientes \
  ".[] | select(.cpfCnpj == \"${CPF_DIGITOS}\")" /api/clientes \
  "$(jq -nc --arg cpf "$CPF_CLIENTE" --arg email "$EMAIL_CLIENTE" '{cpfCnpj: $cpf, nome: "João da Silva", email: $email, telefone: "11999998888"}')")

PLACA_NORMALIZADA=$(tr "[:lower:]" "[:upper:]" <<<"${PLACA//-/}")
VEICULO_ID=$(buscar_ou_criar "/api/veiculos/cliente/${CLIENTE_ID}" \
  ".[] | select(.placa == \"${PLACA_NORMALIZADA}\")" /api/veiculos \
  "$(jq -nc --arg placa "$PLACA" --arg cliente "$CLIENTE_ID" '{placa: $placa, marca: "Toyota", modelo: "Corolla", anoFabricacao: 2022, clienteId: $cliente}')")

SERVICO_ID=$(buscar_ou_criar /api/servicos \
  '.[] | select(.nome == "Troca de óleo")' /api/servicos \
  '{"nome": "Troca de óleo", "descricao": "Troca de óleo e filtro", "precoUnitario": 150.00, "tempoEstimadoHoras": 1}')

PECA_ID=$(buscar_ou_criar /api/pecas \
  '.[] | select(.nome == "Filtro de óleo")' /api/pecas \
  '{"nome": "Filtro de óleo", "descricao": "Filtro de óleo Mann W712", "precoUnitario": 35.00, "quantidadeEstoque": 200, "estoqueMinimo": 2}')

for ((i = 1; i <= TOTAL_OS; i++)); do
  ABERTURA=$(chamar POST /api/ordens "$(jq -nc --arg c "$CLIENTE_ID" --arg v "$VEICULO_ID" '{clienteId: $c, veiculoId: $v}')")
  printf '  OS %d/%d  %s  saga %s\n' "$i" "$TOTAL_OS" "$(jq -r .id <<<"$ABERTURA")" "$(jq -r .sagaId <<<"$ABERTURA")"
  sleep "$PAUSA"
done

echo
echo "clienteId=${CLIENTE_ID}"
echo "veiculoId=${VEICULO_ID}"
echo "servicoId=${SERVICO_ID}"
echo "pecaId=${PECA_ID}"
echo "As ordens abertas seguem pela saga: o diagnóstico começa no Execution Service (POST /api/execucoes/{osId}/diagnostico/inicio)."
