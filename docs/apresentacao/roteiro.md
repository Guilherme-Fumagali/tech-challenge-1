# Roteiro — Tech Challenge Fase 2 (vídeo ≤ 15 min)

Dois apresentadores. Tempo alvo **14:30**, teto rígido **15:00** — sobra ~30s de folga.

Os nomes abaixo são placeholders: **P1 = Danilo**, **P2 = Guilherme**. Troque se a divisão for outra.

Slides: [`slides.html`](slides.html) — abra no navegador, setas/espaço para navegar, tecla `N` mostra
estas falas na própria tela.

## Antes de gravar (checklist)

- [ ] `docker compose up` **não** pode estar rodando (conflito de porta 8080 com o port-forward).
- [ ] Docker Desktop ligado.
- [ ] Cluster kind **já criado** (`cd infra/environments/local && terraform apply`). O apply do zero
      leva ~4 min por causa do `docker build` — não cabe no vídeo. Grave a tela do apply à parte e
      corte, ou mostre o cluster já de pé e o `terraform apply` retornando "No changes".
- [ ] `k6` instalado (`k6 version`).
- [ ] Terminal com fonte grande (≥ 16pt) — o vídeo é assistido em tela pequena.
- [ ] MailHog aberto em `http://localhost:8025`.
- [ ] Swagger aberto em `http://localhost:8080/swagger-ui.html`.

---

## S1 · Abertura — **P1** — 0:00 → 0:40

> Boa tarde. Somos o Danilo e o Guilherme, e este é o Tech Challenge da Fase 2: a API de gestão de
> uma oficina mecânica de médio porte.
>
> Na Fase 1 entregamos o MVP em Clean Architecture. A Fase 2 pediu duas coisas: evoluir as regras
> de negócio e entregar a cadeia de infraestrutura inteira — container, Kubernetes, Terraform e
> CI/CD. É isso que vamos mostrar, e vamos mostrar **rodando**.

## S2 · O que a Fase 2 pediu — **P1** — 0:40 → 1:20

> Cinco entregas. Três regras novas de domínio; a aplicação containerizada; um cluster Kubernetes
> com escalabilidade automática; a infraestrutura descrita em Terraform; e uma esteira de CI/CD
> que vai do commit ao deploy.
>
> Eu cubro a aplicação, o Guilherme cobre a infraestrutura. Cada um com uma demo ao vivo.

## S3 · Evolução do domínio — **P1** — 1:20 → 2:40

> A primeira regra: a **listagem de ordens de serviço é ordenada por prioridade de negócio**, não
> por data. Em Execução vem antes de Aguardando Aprovação, que vem antes de Em Diagnóstico, que vem
> antes de Recebida. Dentro do mesmo status, as mais antigas primeiro — quem esperou mais, aparece
> antes.
>
> Segunda: OS **Finalizada e Entregue somem da listagem** — exclusão lógica. O registro continua no
> banco, para o relatório de tempo médio; só não polui a fila de trabalho de quem está no balcão.
>
> Terceira, e a mais interessante: **aprovação externa por token**. Quando o orçamento é gerado, a
> OS ganha um token único, de uso único e com expiração, e o cliente recebe **um e-mail de verdade**
> com esse token. Ele aprova ou reprova sem ter login no sistema — o endpoint é público, mas só
> aceita aquele token, para aquela OS, uma vez só. É o link do e-mail, que todo mundo já conhece.
>
> E reprovar não é só mudar um status: **estorna o estoque** das peças que já tinham sido baixadas.

## S4 · Clean Architecture e o ACL — **P1** — 2:40 → 3:50

> A arquitetura não mudou, e isso é o ponto: as três regras novas entraram sem tocar em
> infraestrutura.
>
> O domínio é Java puro — zero Spring. A camada de aplicação orquestra os casos de uso. A
> infraestrutura tem Spring, JPA, REST, Security.
>
> O envio de e-mail é o exemplo mais claro. No Context Map, a notificação é um **sistema externo**,
> e a fronteira é um **Anti-Corruption Layer**: uma porta de saída, `NotificacaoService`, escrita na
> linguagem do negócio. Dois adaptadores a implementam — um que só loga, outro que manda SMTP de
> verdade — e a escolha é uma *property*, não uma decisão de código.
>
> Na Fase 1 o canal ficou como Hot Spot em aberto porque o negócio ainda não tinha decidido. Na
> Fase 2 plugamos o SMTP. **Nenhuma regra de negócio foi alterada** — só apareceu um adaptador novo.
> É o ACL fazendo o trabalho dele.

## S5 · DEMO 1 — a API rodando — **P1** — 3:50 → 6:20

**Roteiro da tela** (ensaiar até sair em 2:30 — é o trecho mais fácil de estourar):

1. `POST /api/auth/login` → pega o JWT. *"Rota administrativa exige token."*
2. Abrir uma OS já com cliente e veículo criados (**deixe-os prontos antes de gravar**).
3. `POST /{id}/iniciar-diagnostico` → adicionar um serviço e uma peça.
   *"Repare no estoque da peça caindo — decremento atômico, ACID, sem race condition."*
4. `POST /{id}/gerar-orcamento` → status vai para Aguardando Aprovação.
5. **Trocar para o MailHog** — o e-mail chegou, com o token. *"Isto é o que o cliente recebe."*
6. `POST /{id}/aprovar-externo` com o token, **sem header Authorization**.
   *"Sem login. Só o token."* → status Em Execução.
7. Repetir o mesmo POST → **falha**. *"Uso único. O token já queimou."*
8. `GET /api/ordens` → mostrar a ordenação por prioridade.

> Passo a bola pro Guilherme, que pega isso e coloca num cluster.

## S6 · Containerização — **P2** — 6:20 → 7:00

> A imagem é um Dockerfile **multi-stage**: o primeiro estágio compila com Maven, o segundo carrega
> só o JRE e o JAR. O que vai pra produção não tem Maven, não tem código-fonte, não tem cache do
> build — e roda como **usuário não-root**, com healthcheck.
>
> Para o dia a dia, um `docker compose up` sobe API, Postgres e MailHog.

## S7 · Kubernetes — **P2** — 7:00 → 8:10

> Um namespace só. Dentro: o Postgres, o MailHog e a API.
>
> A API tem **readiness e liveness probes** separadas — a readiness segura o tráfego até o Spring
> subir, a liveness reinicia o pod se ele travar. Tem requests e limits de CPU e memória, e é por
> isso que o autoscaling funciona: **sem `requests`, o HPA não tem denominador** e não calcula
> porcentagem nenhuma.
>
> O HPA vai de **2 a 8 réplicas**, mirando 70% de CPU e 80% de memória. Os dois manifestos de
> `secret.yaml` no repositório têm só `REPLACE_ME` — os valores reais são renderizados pelo
> Terraform. **Nunca há segredo commitado.**
>
> E o `metrics-server` precisou ser adicionado à mão: nem o kind nem o EKS o trazem, e sem ele o
> HPA fica lendo `<unknown>` para sempre.

## S8 · Terraform — dois ambientes — **P2** — 8:10 → 9:00

> Dois cenários, os **mesmos manifestos** de Kubernetes.
>
> O **local** sobe um cluster kind, com Postgres dentro do cluster. Custo zero. É o que vou mostrar
> daqui a pouco.
>
> O **AWS** provisiona tudo do zero: VPC, subnets, Internet Gateway, IAM, o cluster EKS e um
> PostgreSQL gerenciado no RDS. A diferença entre os dois é só onde está o banco e de onde vem a
> imagem — o resto é o mesmo YAML.
>
> Escolhas de custo mínimo no cenário AWS: sem NAT Gateway, instâncias pequenas, single-AZ. É um
> ambiente efêmero, para subir, gravar e destruir.

## S9 · O ovo e a galinha do state — **P2** — 9:00 → 9:50

> Um detalhe que vale a pena contar, porque é uma armadilha clássica.
>
> O state do Terraform mora num bucket S3, com lock no DynamoDB. A pergunta é: **quem cria o
> bucket?**
>
> Se for o próprio Terraform, o primeiro apply — o que cria o bucket — ainda não tem bucket onde
> guardar o state. Ele ficaria em disco, e um state em disco morre junto com o runner do CI. Na
> execução seguinte o Terraform esqueceria que o bucket existe e tentaria criá-lo de novo.
>
> A causa é tratar um **pré-requisito** como se fosse um recurso. Então o bucket e a tabela são
> criados por um script de AWS CLI **idempotente**, que não tem state nenhum: pode rodar dez vezes,
> converge sempre no mesmo lugar. E aí ele roda tranquilo num runner efêmero.
>
> O Terraform continua dono de tudo que o desafio pede. Só a prateleira onde ele guarda o próprio
> state é que fica de fora.

## S10 · CI/CD — **P2** — 9:50 → 11:00

> A cada push: build, testes unitários e de integração com Testcontainers, cobertura no JaCoCo,
> OWASP Dependency-Check e o quality gate do SonarCloud.
>
> Passando, a imagem é construída e publicada no GHCR, e o deploy vai pro cluster.
>
> Duas decisões que valem a menção. Primeira: o Actions se autentica na AWS via **OIDC** — não
> existe access key estática guardada em secret. O GitHub troca um token de curta duração por uma
> role que **só este repositório** consegue assumir.
>
> Segunda: tudo que **cobra dinheiro** — o apply, o destroy — passa por um GitHub Environment com
> **aprovação manual obrigatória**. A pipeline para e espera um humano. Depois de um único passo
> local, que é criar essa primeira credencial — e esse é impossível de automatizar, porque para
> criar a primeira credencial você já precisaria de uma —, **todo o resto é um clique no Actions**.

## S11 · DEMO 2 — cluster e autoscaling — **P2** — 11:00 → 13:30

**Roteiro da tela** (2:30 — o HPA é a estrela, reserve tempo pra ele):

1. `terraform apply` no ambiente local. *"Ele cria o cluster, builda a imagem, injeta no nó e
   aplica os manifestos."* (Se estiver pré-criado, mostre o apply retornando sem mudanças e
   comente que a criação do zero leva ~4 min.)
2. `kubectl get pods -n oficina` → API, Postgres, MailHog de pé. **Duas réplicas** da API.
3. `kubectl port-forward svc/oficina-api 8080:80 -n oficina` → `curl /actuator/health` → `UP`.
   *"É a mesma API da primeira demo, agora num cluster."*
4. **Tela dividida**: `kubectl get hpa -n oficina -w` de um lado, `k6 run` do outro.
5. Subir a carga. **Narrar o HPA acordando**: `<unknown>` → percentual real → passa de 70% →
   réplicas sobem de 2 para 4, 6, 8.
6. *"Oito réplicas, o teto que definimos. Quando a carga cair, ele volta a duas sozinho — com um
   atraso proposital, pra não ficar batendo pra cima e pra baixo."*

## S12 · Qualidade e trade-offs — **P1 + P2** — 13:30 → 14:10

> **P2:** Na esteira: quality gate do SonarCloud, cobertura pelo JaCoCo e OWASP Dependency-Check a
> cada push. Quatro CVEs seguem suprimidas com justificativa — são de Tomcat e Spring Security, e
> ainda **não existe patch publicado**. Estão documentadas, com data, e são reavaliadas a cada fase.
>
> **P1:** Três débitos da Fase 1 foram quitados: o MapStruct que estava declarado e nunca foi usado
> saiu do projeto; o construtor de nove parâmetros virou um record; e o stub de notificação virou
> SMTP de verdade.

## S13 · Fechamento — **P1** — 14:10 → 14:30

> Regras novas sem tocar em infraestrutura, porque a arquitetura aguentou. Infraestrutura
> declarada, versionada e destruível com um clique. E a esteira inteira, do commit ao cluster.
>
> Obrigado.

---

## Se estourar o tempo

Corte nesta ordem, de cima para baixo:

1. **S9** (o ovo e a galinha do state) — é a parte mais bonita tecnicamente e a mais dispensável
   para a nota. Vale 50s.
2. **S12** — junte numa frase só dentro do fechamento.
3. Na **Demo 1**, corte o passo 7 (token de uso único) — ele é ótimo, mas o passo 6 já prova a
   regra.

**Não corte** a Demo 2: autoscaling ao vivo é requisito explícito do enunciado.
