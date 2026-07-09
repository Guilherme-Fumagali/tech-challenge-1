# Kubernetes — Fundamentos e Tópicos Avançados

> Fonte: `artefatos-tech-challenge-2/aulas/kubernetes-1/` (Aulas 1–8, disciplina "Kubernetes") e `artefatos-tech-challenge-2/aulas/kubernetes-2/` (Aulas 1–9, disciplina "Kubernetes Parte II"), curso PosTech FIAP. Autor do material: Thiago Adriano. Síntese condensada para consulta durante a Fase 2 do `oficina-api` — não substitui os PDFs originais em caso de dúvida pontual.

## Contexto e motivação (kubernetes-1, Aula 1)

Dois problemas recorrentes em infraestrutura de software motivam o Kubernetes:

- **Gestão de múltiplos ambientes** (dev/teste/produção) — manter paridade de configuração, capacidade e rede entre eles é caro e complexo.
- **Escalabilidade** — capacidade de crescer/encolher a infraestrutura conforme a demanda (ex.: picos de Black Friday). Provisionar hardware fixo para o pico é caro; não provisionar o suficiente derruba o serviço.

**Containers vs. VMs**: uma VM virtualiza hardware + SO completo dentro de um host, compartilhando apenas o hardware. Um container compartilha o hardware **e** o kernel do SO hospedeiro, carregando somente as libs/binários da aplicação — por isso é muito mais leve. Isso viabiliza **escalabilidade horizontal** (criar/destruir containers sob demanda), diferente da escalabilidade vertical (aumentar recursos de uma máquina existente).

O Kubernetes (K8s) automatiza o que seria manual e caro: criar/destruir containers a partir de imagens, substituir containers que falham, e gerenciar a infraestrutura operacional do cluster.

---

## Fundamentos (kubernetes-1)

### Arquitetura do cluster (Aula 1, Aula 3)

Um cluster é um grupo de **nodes**:

- **Master node** — componente central; toma decisões de onde/como executar os Pods; expõe a API REST do Kubernetes.
- **Worker node** — executa os Pods de fato.
- **etcd** — banco de dados distribuído com a configuração e o estado do cluster.
- **kubelet** — agente em cada node que gerencia os Pods daquele node.
- **kube-proxy** — encaminha tráfego de rede para os Pods.

A interação com o cluster acontece via **API REST** (GET/POST/PUT/DELETE, organizada em grupos de recursos como `core`, `apps`, `batch`, `rbac.authorization.k8s.io` etc.) ou, na prática, via **kubectl** — o CLI cliente do master node, que lê `kubeconfig` (endereço da API, credenciais, contexto) para saber a qual cluster se conectar.

Ambiente local de estudo/dev: `kubectl` + `Docker` + `Minikube` (implementação leve de K8s com um nó único, roda em VM local).

### Pods, rótulos e anotações (Aula 3)

**Pod** = unidade mínima do Kubernetes; abstrai um processo em execução, pode conter 1+ containers que compartilham rede e armazenamento. Pods são **efêmeros** — criados e destruídos conforme necessário — e recebem IP interno do cluster ao subir.

- **Rótulos (labels)**: pares chave-valor (`app: myapp`) usados para identificar/selecionar objetos (Pods, Services, etc.) em operações de gerenciamento (escalonamento, exclusão). Seletores de labels são a base de como Services, ReplicaSets e Deployments "encontram" seus Pods.
- **Anotações (annotations)**: pares chave-valor opcionais para metadados (autor, docs, políticas) — não usadas para seleção, só documentação/auditoria.

Pods são definidos em manifestos YAML/JSON: `apiVersion`, `kind`, `metadata` (nome, labels), `spec.containers` (name, image, ports).

### Services e ConfigMaps (Aula 4)

**Service** expõe um conjunto de Pods como um único ponto de entrada, balanceando tráfego entre eles via `selector` (labels). Três tipos:

| Tipo | Alcance | Uso típico |
|---|---|---|
| `ClusterIP` (padrão) | Interno ao cluster | Comunicação serviço-a-serviço |
| `NodePort` | IP do node + porta fixa | Acesso externo simples |
| `LoadBalancer` | Balanceador externo (cloud) | Exposição pública com balanceamento |

**ConfigMap** separa configuração do artefato de deploy — centraliza variáveis de ambiente/arquivos de config fora da imagem do container. Pode ser injetado como env vars (`envFrom`/`configMapKeyRef`), arquivo montado, ou argumento de linha de comando. Fluxo típico: `kubectl create configmap <nome> --from-literal=...` ou via manifesto YAML, depois referenciado no `spec.containers.envFrom` do Deployment.

### ReplicaSets e Deployments (Aula 5)

**ReplicaSet**: garante que N réplicas de um Pod (selecionadas por label) estejam sempre rodando; substitui automaticamente réplicas que falham.

**Deployment**: camada acima do ReplicaSet — gerencia réplicas **e** versionamento/atualização da aplicação:

- **Rolling update**: ao atualizar a imagem, cria gradualmente novas réplicas e substitui as antigas sem downtime.
- **Rollback**: reverte para a versão anterior em caso de problema.
- **Dimensionamento horizontal**: ajusta o número de réplicas conforme demanda.

Na prática, usa-se quase sempre **Deployment**, não ReplicaSet diretamente, pelo controle de versão/rollout que ele oferece.

### Volumes, PV, PVC e StorageClass (Aula 6)

Containers são efêmeros por padrão — dados somem se o container reiniciar. Soluções de armazenamento:

- **Volume (VOL)**: ciclo de vida ligado ao Pod. Tipos comuns: `emptyDir` (criado e destruído junto com o Pod; útil para cache/arquivos temporários compartilhados entre containers do mesmo Pod), `hostPath` (monta diretório do node no container), `persistentVolumeClaim`.
- **PersistentVolume (PV)**: sobrevive ao Pod; provisionado pelo admin do cluster (NFS, iSCSI, hostPath, EBS etc.).
- **PersistentVolumeClaim (PVC)**: "pedido" de armazenamento feito pelo Pod, com modos de acesso: `ReadWriteOnce` (1 node, leitura/escrita), `ReadOnlyMany` (N nodes, leitura), `ReadWriteMany` (N nodes, leitura/escrita).
- **StorageClass (SC)**: define perfis de armazenamento (Standard, SSD, SAN) para provisionamento dinâmico de PVs conforme a necessidade da aplicação.

### Probes: Liveness, Readiness, Startup (Aula 7)

Definidas no manifesto do Pod/Deployment, garantem confiabilidade em produção — sem elas, containers podem falhar silenciosamente:

| Probe | Pergunta que responde | Ação em caso de falha |
|---|---|---|
| **Liveness** | "A aplicação está viva/respondendo?" | Kubernetes **reinicia o Pod** |
| **Readiness** | "A aplicação está pronta para receber tráfego?" | Kubernetes **remove o Pod do balanceador** (não reinicia) |
| **Startup** | "A aplicação terminou de inicializar?" | Kubernetes aguarda antes de rodar as outras probes; se falhar, reinicia o Pod |

Padrão: requisição HTTP GET a um endpoint, esperando `200 OK` (também suporta TCP e Exec). Boas práticas: usar as três probes, endpoints distintos por probe, ajustar intervalos conforme criticidade, monitorar falhas de probe (ex.: via Prometheus).

### Horizontal Pod Autoscaler — HPA (Aula 8)

Ajusta automaticamente o número de réplicas de um Deployment com base em métricas de utilização (CPU, memória, métricas customizadas, métricas externas como Prometheus, E/S de disco). Fluxo: monitora métricas → compara com limite (`target`) → aumenta réplicas se acima do limite, diminui se abaixo, respeitando `minReplicas`/`maxReplicas`.

Exemplo do material (usa `autoscaling/v1`, que só suporta CPU):

```yaml
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: my-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-deployment
  minReplicas: 1
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

Validação: `kubectl get hpa` (antes/depois de gerar carga com K6 ou JMeter).

> **Nota prática**: `autoscaling/v1` só escala por CPU. Para escalar também por **memória** — exigido pela Fase 2 — é necessário `autoscaling/v2`, que permite uma lista de `metrics` (`type: Resource`, `name: cpu` e `name: memory`). Ver esqueleto na seção de aplicação abaixo.

---

## Tópicos avançados (kubernetes-2)

### Jobs e CronJobs (Aula 1)

Para cargas de trabalho **batch** (não reativas), diferente de Pods/Deployments de longa duração:

- **Job**: executa um trabalho até a conclusão (ex.: backup, relatório) e depois remove os Pods automaticamente (`restartPolicy: Never`, `ttlSecondsAfterFinished` controla quanto tempo manter o Job após concluído).
- **CronJob**: agenda a execução de um Job com sintaxe cron (`schedule: "0 0 * * *"` = diariamente à meia-noite).

### Observabilidade com EFK — Elasticsearch, Fluentd, Kibana (Aula 2)

O log nativo do Kubernetes não é suficiente porque arquivos de log devem sobreviver ao ciclo de vida do Pod/container. Três abordagens de captura: direto pela aplicação, agente no node, ou Pod auxiliar (sidecar). Recomendação do material: não reinventar a roda — usar um ecossistema consolidado.

- **Elasticsearch**: indexação e busca (implantado como `StatefulSet` no exemplo, pois precisa de armazenamento persistente e identidade estável).
- **Fluentd**: coleta logs de várias fontes e envia ao Elasticsearch (implantado como `DaemonSet`, para rodar em todo node e capturar logs no nível do host; requer `ServiceAccount` + `ClusterRole` + `ClusterRoleBinding` com permissão de `get/list/watch` sobre `pods`/`namespaces`).
- **Kibana**: visualização (Deployment + Service, consultando o Elasticsearch via DNS interno do cluster).

Esse exemplo introduz na prática `StatefulSet`, `DaemonSet` e os objetos de RBAC (retomados na Aula 8).

### Helm (Aula 3)

Gerenciador de pacotes do Kubernetes — empacota todos os recursos necessários para um app (Pods, Services, volumes, Ingress) em um **chart**.

- Instalação: baixar binário, mover para `/usr/local/bin` (Linux) ou PATH (Windows); validar com `helm version`.
- Repositórios: `helm repo add stable <url>` + `helm repo update`.
- Criar chart: `helm create <nome>` → gera `Chart.yaml` (metadados) e `values.yaml` (valores default de configuração).
- Empacotar: `helm package` → gera `.tgz` instalável no cluster.
- Benefícios: versionamento de releases, configuração flexível via `values.yaml`, reuso/compartilhamento de charts entre times.

### Kubernetes gerenciado na nuvem — Amazon EKS (Aula 4)

Rodar K8s on-premise/local exige provisionar servidores e assumir toda a gestão (upgrades, patches, segurança). Serviços gerenciados (EKS na AWS, além de equivalentes em Azure/GCP/IBM Cloud) eliminam esse ônus: a cloud gerencia o control plane e a infraestrutura subjacente; o time gerencia só os workloads — usando as mesmas ferramentas e APIs do Kubernetes padrão.

Configuração de um cluster EKS cobre três eixos:

- **Rede**: `Amazon VPC CNI` (padrão, Pods recebem IP da VPC) ou `Calico` (rede de terceiros, com segregação de tráfego entre namespaces).
- **Control plane**: versão do Kubernetes, autenticação/autorização.
- **Worker nodes**: tipo/tamanho de instância, quantidade, storage.

### Caso prático — migração de IIS para EKS (Aula 5)

Estudo de caso de ponta a ponta, útil como referência de estrutura de manifestos:

1. Avaliar a aplicação legada (dependências, requisitos, desenhar o fluxo).
2. Criar `Dockerfile` com imagem base apropriada, testar localmente com Pods.
3. Criar objetos Kubernetes: **Deployment** (réplicas + imagem), **Service** (`type: LoadBalancer` expondo a porta), **Ingress** (roteamento por host/path para o Service).

```yaml
# Ingress — exemplo do material
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-nodejs-app-ingress
spec:
  rules:
  - host: my-nodejs-app.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: my-nodejs-app-service
            port:
              number: 80
```

### HPA aplicado + testes de carga com K6 (Aula 6)

Reforça o HPA em contexto de produção/EKS: sem autoscaling, picos de tráfego (ex.: promoção em e-commerce) exigem intervenção manual, lenta e sujeita a erro. Com HPA, o cluster reage sozinho dentro de `minReplicas`/`maxReplicas`.

**K6** é a ferramenta de teste de carga usada para simular tráfego e validar o HPA na prática (`k6 run --vus 10 --duration 30s test.js`) — dispara requisições, observa `kubectl get hpa` escalando réplicas para cima, depois para baixo quando a carga cessa.

### CI/CD (Aula 7)

Esteira de integração/entrega contínua usando Git + pipeline (o material usa Azure DevOps/GitHub Actions como exemplo) + Kubernetes como alvo de deploy:

- **CI (Integração Contínua)**: a cada push, clona o repo, instala dependências, roda testes automatizados.
- **CD (Entrega/Implantação Contínua)**: implanta automaticamente em ambientes (dev → homologação → produção), tipicamente por branch/tag.

Estrutura conceitual de um pipeline: checkout → build/testes → build de imagem Docker → push para registry → deploy (aplicar manifestos/Helm chart) no cluster.

### RBAC e segurança no EKS (Aula 8)

Duas camadas de controle de acesso:

- **IAM (AWS)**: identidades, grupos e funções (roles) com permissões para operar recursos do EKS (ex.: role que só pode criar/excluir Pods, nada mais).
- **RBAC (Kubernetes)**: `ClusterRole` define **o quê** pode ser feito (verbos `get`/`list`/`watch`/`create`/`delete` sobre quais recursos); `RoleBinding`/`ClusterRoleBinding` associa a role a um usuário/grupo/ServiceAccount.

```yaml
# ClusterRole — permissão de leitura sobre Pods
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: example-role
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "watch", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: example-rolebinding
  namespace: default
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: example-role
subjects:
- kind: User
  name: tadriano
```

Validar: `kubectl auth can-i get pods --as tadriano`.

### Monitoramento em produção — CloudWatch, Prometheus, Grafana (Aula 9)

Complementa o EFK (aula 2) com o ecossistema nativo AWS + open source:

- **Amazon CloudWatch**: coleta métricas/logs/eventos da AWS; `CloudWatch Logs` armazena e pesquisa logs (por grupos/streams); `CloudWatch Logs Insights` permite queries ad hoc para troubleshooting; dashboards e alertas nativos.
- **Prometheus**: monitoramento open source baseado em *pull* (o servidor busca métricas nos alvos em intervalos regulares), armazenadas em banco de séries temporais, consultáveis via linguagem própria (PromQL). Roda no cluster como Pods gerenciados; integra com `kube-prometheus`/`prometheus-operator`.
- **Grafana**: visualização — consome dados do Prometheus (e outras fontes) para dashboards e alertas customizados em tempo real.

---

## Comandos kubectl essenciais

| Comando | Descrição |
|---|---|
| `kubectl version --output=yaml` | Valida instalação/versão do cliente e do servidor |
| `kubectl get pods` / `get svc` / `get deployments` / `get hpa` | Lista recursos e seu status |
| `kubectl describe pod <nome>` | Detalhes e eventos de um recurso (essencial para debug) |
| `kubectl apply -f <arquivo>.yaml` | Cria/atualiza um recurso a partir de um manifesto (idempotente) |
| `kubectl delete pod/deployment/svc <nome>` | Remove um recurso |
| `kubectl run <nome> --image=<imagem> --port=<porta>` | Cria um Pod avulso rapidamente (uso exploratório) |
| `kubectl logs <pod>` | Exibe logs de um container |
| `kubectl exec -it <pod> -- /bin/sh` | Shell interativo dentro de um container |
| `kubectl scale deployment <nome> --replicas=N` | Escala manualmente um Deployment |
| `kubectl rollout status/undo deployment <nome>` | Acompanha ou reverte um rolling update |
| `kubectl create configmap <nome> --from-literal=chave=valor` | Cria ConfigMap via linha de comando |
| `kubectl create secret generic <nome> --from-literal=chave=valor` | Cria Secret via linha de comando |
| `kubectl auth can-i <verbo> <recurso> --as <usuário>` | Testa permissão RBAC de um usuário |
| `kubectl top pods` / `top nodes` | Uso de CPU/memória (requer `metrics-server` instalado no cluster) |
| `minikube start` | Sobe um cluster local de nó único para estudo/dev |
| `helm install/upgrade/uninstall <release> <chart>` | Ciclo de vida de um chart Helm |

---

## Aplicação no `oficina-api`

Esqueleto **de ponto de partida** para a pasta `/k8s`, combinando os conceitos das aulas com o estado real do projeto (Spring Boot 3.3 / Java 21, `Dockerfile` multi-stage já existente expondo a porta `8080`, variáveis de ambiente `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `PORT` já lidas via `application.yml`). **Ajustar antes de aplicar em cluster real** — em especial: nome/registry da imagem, thresholds de HPA, e o endpoint de probes (o projeto ainda não expõe `/actuator/health`; avaliar adicionar `spring-boot-starter-actuator` ou usar uma probe TCP na porta 8080 como fallback).

### `k8s/configmap.yaml` — configuração não sensível

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: oficina-api-config
data:
  DB_URL: "jdbc:postgresql://oficina-db:5432/oficina"  # host = Service do Postgres no cluster
  PORT: "8080"
```

### `k8s/secret.yaml` — placeholder (NÃO commitar valores reais)

```yaml
# Placeholder de exemplo. Em produção, gerar via:
#   kubectl create secret generic oficina-api-secret \
#     --from-literal=DB_USER=... --from-literal=DB_PASS=... --from-literal=JWT_SECRET=...
# ou via um gerenciador externo (Sealed Secrets, External Secrets Operator, SSM Parameter Store).
apiVersion: v1
kind: Secret
metadata:
  name: oficina-api-secret
type: Opaque
stringData:
  DB_USER: "REPLACE_ME"
  DB_PASS: "REPLACE_ME"
  JWT_SECRET: "REPLACE_ME_32_CHARS_MIN"
```

### `k8s/deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: oficina-api
  labels:
    app: oficina-api
spec:
  replicas: 2
  selector:
    matchLabels:
      app: oficina-api
  template:
    metadata:
      labels:
        app: oficina-api
    spec:
      containers:
      - name: oficina-api
        image: <registry>/oficina-api:<tag>   # ex.: ghcr.io/<org>/oficina-api:latest — ajustar conforme [[github-actions]]
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: oficina-api-config
        env:
        - name: DB_USER
          valueFrom:
            secretKeyRef:
              name: oficina-api-secret
              key: DB_USER
        - name: DB_PASS
          valueFrom:
            secretKeyRef:
              name: oficina-api-secret
              key: DB_PASS
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: oficina-api-secret
              key: JWT_SECRET
        resources:
          # obrigatório: HPA de CPU/memória só funciona com "requests" definidos
          requests:
            cpu: "250m"
            memory: "384Mi"
          limits:
            cpu: "750m"
            memory: "768Mi"
        readinessProbe:
          httpGet:
            path: /swagger-ui.html   # substituir por /actuator/health/readiness se actuator for adicionado
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 10
        livenessProbe:
          tcpSocket:
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 15
```

### `k8s/service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: oficina-api
spec:
  selector:
    app: oficina-api
  type: ClusterIP   # trocar para LoadBalancer se exposição externa direta for necessária (ou usar Ingress)
  ports:
  - port: 80
    targetPort: 8080
```

### `k8s/hpa.yaml` — CPU e memória

```yaml
# autoscaling/v2 (não v1) — necessário para escalar por memória além de CPU
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: oficina-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: oficina-api
  minReplicas: 2
  maxReplicas: 8
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

> Pré-requisito para o HPA funcionar: o cluster precisa ter o **metrics-server** instalado (não vem por padrão em todo Minikube/kind). Validar escala gerando carga com K6, conforme a Aula 6 de kubernetes-2, e observando `kubectl get hpa -w`.

Ver também: [[dockerizacao]] (Dockerfile/imagem que o Deployment consome), [[terraform]] (provisionamento do cluster onde estes manifestos serão aplicados), [[github-actions]] (pipeline que builda a imagem e roda `kubectl apply -f k8s/`).
