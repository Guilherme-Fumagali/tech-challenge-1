# Kubernetes — oficina-api

Manifests para rodar a aplicação completa (API + Postgres + MailHog) em qualquer cluster Kubernetes.

## Pré-requisitos

- Um cluster acessível via `kubectl` (local via `kind` — ver `../infra/environments/local` — ou remoto).
- **metrics-server** instalado no cluster (necessário para o HPA funcionar). Em `kind`:
  ```
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
  kubectl patch deployment metrics-server -n kube-system --type=json \
    -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
  ```

## Segredos

Os arquivos `secret.yaml` (`app/` e `database/`) contêm apenas placeholders (`REPLACE_ME`). Antes de aplicar, gere os reais:

```
kubectl create namespace oficina --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic oficina-db-secret -n oficina \
  --from-literal=POSTGRES_USER=oficina \
  --from-literal=POSTGRES_PASSWORD=<senha-real> \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic oficina-api-secret -n oficina \
  --from-literal=DB_USER=oficina \
  --from-literal=DB_PASS=<senha-real> \
  --from-literal=JWT_SECRET=<chave-real-min-32-chars> \
  --from-literal=ADMIN_PASSWORD=<senha-real> \
  --dry-run=client -o yaml | kubectl apply -f -
```

## Aplicar (ordem)

```
kubectl apply -f namespace.yaml
kubectl apply -f database/
kubectl apply -f mailhog/
kubectl apply -f app/
```

> Em ambiente AWS (`../infra/environments/aws`), o Terraform substitui `app/configmap.yaml` e `app/secret.yaml`
> por versões renderizadas com o endpoint do RDS e não aplica `database/` (o banco é o RDS gerenciado, não in-cluster).

## Smoke test

```
kubectl get pods -n oficina -w        # aguardar Ready
kubectl port-forward svc/oficina-api 8080:80 -n oficina
curl http://localhost:8080/actuator/health
```

## Acompanhar o HPA

```
kubectl get hpa -n oficina -w
k6 run -e BASE_URL=http://localhost:8080 loadtest/k6-script.js
```

## Acesso

Sem Ingress configurado (fora de escopo por simplicidade) — use `kubectl port-forward` como acima. Um
`ingress-nginx` local é um stretch opcional caso se queira uma URL fixa para a demo.
