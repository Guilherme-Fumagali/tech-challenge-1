#!/usr/bin/env bash
set -euo pipefail

AMBIENTE=${AMBIENTE:-staging}
CPF_FUNCIONARIO=${CPF_FUNCIONARIO:?informe CPF_FUNCIONARIO}
CPF_CLIENTE=${CPF_CLIENTE:-529.982.247-25}
PLACA=${PLACA:-ABC1D23}
TOTAL_OS=${TOTAL_OS:-30}
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

autenticar() {
  local rota=$1 cpf=$2
  TOKEN=""
  chamar POST "$rota" "$(jq -nc --arg cpf "$cpf" '{cpf: $cpf}')" | jq -r .accessToken
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

avancar() {
  local os=$1
  shift
  for acao in "$@"; do
    case $acao in
      servico) chamar POST "/api/ordens/${os}/servicos" "$(jq -nc --arg id "$SERVICO_ID" '{itemId: $id, quantidade: 1}')" ;;
      peca) chamar POST "/api/ordens/${os}/pecas" "$(jq -nc --arg id "$PECA_ID" '{itemId: $id, quantidade: 1}')" ;;
      *) chamar POST "/api/ordens/${os}/${acao}" ;;
    esac >/dev/null
    sleep "$PAUSA"
  done
}

echo "API: ${API_URL}"
TOKEN=$(autenticar /auth/funcionarios "$CPF_FUNCIONARIO")

CPF_DIGITOS=${CPF_CLIENTE//[^0-9]/}
CLIENTE_ID=$(buscar_ou_criar /api/clientes \
  ".[] | select(.cpfCnpj == \"${CPF_DIGITOS}\")" /api/clientes \
  "$(jq -nc --arg cpf "$CPF_CLIENTE" '{cpfCnpj: $cpf, nome: "João da Silva", email: "joao@email.com", telefone: "11999998888"}')")

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

ORCAMENTO=(iniciar-diagnostico servico peca gerar-orcamento)
for ((i = 1; i <= TOTAL_OS; i++)); do
  OS_ID=$(chamar POST /api/ordens "$(jq -nc --arg c "$CLIENTE_ID" --arg v "$VEICULO_ID" '{clienteId: $c, veiculoId: $v}')" | jq -r .id)
  case $((i % 7)) in
    0) destino=RECEBIDA ;;
    1) destino=EM_DIAGNOSTICO; avancar "$OS_ID" iniciar-diagnostico ;;
    2) destino=AGUARDANDO_APROVACAO; avancar "$OS_ID" "${ORCAMENTO[@]}" ;;
    3) destino=EM_EXECUCAO; avancar "$OS_ID" "${ORCAMENTO[@]}" aprovar ;;
    4) destino=FINALIZADA; avancar "$OS_ID" "${ORCAMENTO[@]}" aprovar concluir ;;
    5) destino=ENTREGUE; avancar "$OS_ID" "${ORCAMENTO[@]}" aprovar concluir entregar ;;
    6) destino=CANCELADA; avancar "$OS_ID" "${ORCAMENTO[@]}" reprovar ;;
  esac
  printf '  OS %2d/%d  %-22s %s\n' "$i" "$TOTAL_OS" "$destino" "$OS_ID"
done

autenticar /auth "$CPF_CLIENTE" >/dev/null

echo
echo "clienteId=${CLIENTE_ID}"
echo "veiculoId=${VEICULO_ID}"
echo "servicoId=${SERVICO_ID}"
echo "pecaId=${PECA_ID}"
