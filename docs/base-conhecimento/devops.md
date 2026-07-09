# DevOps — Base de Conhecimento

> Síntese das 4 aulas do módulo "DevOps (IaC and CI/CD)" — Software Architecture, Fase 2 PosTech FIAP: 01-CI/CD Principais Conceitos, 02-Melhores Práticas com o GitHub, 03-Testes Automatizados com CI, 04-Qualidade do Código-Fonte. Objetivo: referência rápida para decisões de pipeline/qualidade do `oficina-api` (ver [[00-requisitos-fase2]]). Não é transcrição literal — é condensação para consulta. Para detalhes de sintaxe/implementação do GitHub Actions, ver [[github-actions]] (complementar, não duplicado aqui).

## CI/CD — conceitos principais

**Contexto histórico**: o conceito de Integração Contínua (CI) vem do **Extreme Programming (XP)**, metodologia ágil criada por Kent Beck em 1996 — mais de 10 anos antes do termo "DevOps" existir. Uma das "Boas Práticas de XP" já definia que os desenvolvedores devem integrar seu código a um repositório único com frequência (idealmente ao menos uma vez por dia), para eliminar retrabalho e facilitar a localização de erros.

**Fluxo de trabalho manual vs. automatizado** — 4 etapas que existem com ou sem automação:

| Etapa | Descrição |
|---|---|
| Integração do código-fonte | "Juntar" o código de todos os desenvolvedores no mesmo repositório |
| Compilação/build | Transformar o código-fonte em um artefato executável |
| Verificação/testes | Testar se o código está sem erros e integrado corretamente com o das demais pessoas |
| Implantação/deploy | Instalar o software em um servidor para que usuários possam acessá-lo |

Feitas manualmente, essas etapas são repetitivas e suscetíveis a erro humano. A CI propõe automatizá-las para que rodem de forma independente da vontade/disponibilidade do desenvolvedor.

**Definição de CI** (Duvall, 2007): não é apenas "juntar componentes de software" — é a peça central do desenvolvimento, pois garante a integridade do software executando um build a cada mudança. A qualidade do software pode ser verificada checando o resultado da integração mais recente.

**Valores da integração contínua**:
- Redução de riscos — integrar várias vezes ao dia facilita detectar defeitos cedo.
- Redução de processos manuais repetitivos — economia de tempo, custo e esforço; menos erro humano.
- Software implantável a qualquer momento — como cada alteração já é testada, erros são comunicados de imediato ao desenvolvedor, não numa fase posterior que atrasaria a entrega.
- Maior visibilidade do projeto — métricas automatizadas mostram qualidade e requisitos pendentes.
- Maior confiança da equipe no software produzido — feedback imediato quando algo quebra.

**CI vs. Entrega Contínua vs. Deploy Contínuo** — "CD" no jargão "CI/CD" na verdade engloba dois processos distintos:
- **Entrega contínua (Continuous Delivery)**: garante que toda mudança *poderia* ser implantada em produção (pacote testado e armazenado em repositório de artefatos), mas a equipe pode optar por não implantar (ex.: por motivo comercial).
- **Deploy contínuo (Continuous Deployment)**: cada pacote que passa em todos os critérios da esteira (qualidade, segurança etc.) é implantado **automaticamente** em produção, sem intervenção humana.
- Deploy contínuo pressupõe entrega contínua — não existe um sem o outro.

**Trabalhar em pequenos lotes**: lotes grandes têm alto custo fixo de entrega. Trabalhar em pequenos lotes reduz o tempo até obter feedback, facilita triagem/correção de problemas, aumenta eficiência/motivação e reduz o risco de cada mudança individual — é um objetivo central da entrega contínua.

## Boas práticas com GitHub

**Por que versionamento importa**: o código-fonte é a matéria-prima do software — perdê-lo (parcial ou totalmente) implica recriar tudo do zero. Tratar o versionamento com o mesmo rigor que um processo financeiro/bancário crítico.

**Versionamento centralizado vs. distribuído**:

| | Centralizado (CVS, SVN) | Distribuído (Git/GitHub, Bitbucket, GitLab) |
|---|---|---|
| Modelo | Um único servidor com o código versionado | Cada dev tem cópia completa do repositório + histórico |
| Vantagem | Visibilidade total do que todos fazem; controle de acesso centralizado | Sem ponto único de falha (qualquer cópia local restaura o servidor); múltiplos fluxos de trabalho possíveis |
| Desvantagem | Ponto único de falha; perda total se o servidor falhar sem backup | Exige mais disciplina da equipe; mais conflitos de merge em times grandes trabalhando em paralelo |

O modelo distribuído é o indicado para CI, por permitir que múltiplos times (dev, QA, infra) trabalhem em paralelo sobre o mesmo histórico.

**Workflows de branching** — critérios de escolha: produtividade (acelera ou trava?), coerência com o tamanho da equipe, praticidade (fácil desfazer erros?), simplicidade (sem sobrecarga cognitiva desnecessária).

| | **GitHub Flow** | **OneFlow** | **GitFlow** |
|---|---|---|---|
| Ideia central | Lightweight; qualquer coisa em `main` pode ser entregue | Toda nova versão em produção parte da release anterior; uma única branch de produção | Duas branches principais: `master` (produção) e `develop` (em andamento) |
| Feature branches | Sim, a partir de `main` | Sim | Sim |
| Release branches | Não | Sim (temporárias) | Sim |
| Merge/rebase | Merge via pull request (obrigatório) | Rebase opcional; merge opcional | Merge sem fast-forward; rebase não |
| Hotfix | — (corrige direto e reverte se preciso) | Branch a partir da última release | Branch dedicada `hotfix` |
| Vantagem | Ideal para deploy contínuo / versão única em produção | Histórico mais limpo e legível; flexível | Mais conhecido e difundido; separa claramente dev de produção |
| Desvantagem | Produção pode ficar instável; não serve para múltiplas versões em produção simultâneas | Não recomendado com processo de homologação formal | Mais burocrático; overhead de branches |

Regra prática do GitHub Flow: **pull request é etapa obrigatória** antes do merge em `main`, servindo também como ponto de **code review**. Qualquer coisa fora de `main` é considerado "Work in Progress".

**Merge vs. rebase**: resultado final equivalente, mas rebase reescreve o histórico de forma linear/limpa (move os commits para o início da branch de destino), enquanto merge cria um commit de junção que preserva a árvore real de como o trabalho aconteceu (mais seguro, não reescreve histórico, mas deixa o log mais "ramificado").

> Para a Fase 2, recomenda-se **GitHub Flow** (ou uma variação simples dele): pipeline com deploy a partir de `main`, branches de feature curtas, PR obrigatório com CI rodando antes do merge — alinhado ao critério de "amigável para deploy contínuo" e ao tamanho/prazo do projeto acadêmico. GitFlow tende a ser overhead desnecessário para um time pequeno sem múltiplas versões em produção.

## Testes automatizados em CI

**Testes no DevOps vs. modelo tradicional**: DevOps aproxima desenvolvimento, teste e release — a própria equipe ágil é responsável pelas três frentes. Testadores não cuidam só de testes funcionais/features: também cobrem testes de operação, desempenho, segurança básica e monitoramento/análise de dados de produção. O ponto central: **teste é automatizado e contínuo**, disparado a cada alteração de código via pipeline (workflow).

**Teste automático ≠ teste contínuo**:
- **Automático**: executado por estímulo humano (rodar comando/IDE localmente).
- **Contínuo**: disparado automaticamente a cada push/alteração no repositório, como etapa de um workflow — se falhar, a próxima etapa do pipeline não avança.

**TDD (Test Driven Development)**: único processo genuinamente manual e essencial no fluxo — a pessoa desenvolvedora escreve primeiro um teste que falha, depois escreve o código mínimo para fazê-lo passar, e refatora. É o ponto de partida da automação: sem essa disciplina inicial, times acabam mantendo checklists manuais de teste (prática não recomendável hoje).

**Testes técnicos como base**: testes de integração, de componente e unitários representam ~90% de todos os testes que deveriam ser executados — porque um erro pego nessa fase custa muito menos (tempo/esforço/dinheiro) do que um encontrado em homologação ou produção.

**Pipeline de testes — princípio chave**: a automação deve rodar exatamente os mesmos comandos que a pessoa desenvolvedora rodaria localmente (principalmente teste unitário), garantindo paridade entre execução local e CI.

Comandos de build/teste por linguagem (citados nas aulas):

| Linguagem/Stack | Comando |
|---|---|
| Java (Maven) | `mvn test` / `mvn verify` |
| Java (Ant) | Ant build |
| .NET | `dotnet test` |
| Node.js | `npm test` |
| Python | `pytest` |

Exemplo de step de CI (Java/Maven, GitHub Actions):

```yaml
steps:
  - uses: actions/checkout@v3
  - uses: actions/setup-java@v3
    with:
      java-version: '17'
      distribution: 'temurin'
  - name: Run the Maven verify phase
    run: mvn --batch-mode --update-snapshots verify
```

**Estratégia recomendada**: testar tudo o que for possível — do teste unitário (mais ligado à etapa de CI) até testes funcionais em homologação/produção — cobrindo o espectro completo do pipeline de entrega.

## Qualidade de código-fonte

**Definição**: qualidade de código-fonte mede quão bem escrito, organizado, eficiente e confiável é o código — indo além de "funciona", cobrindo requisitos funcionais e não funcionais de forma sustentável.

**Características de código de alta qualidade**:
- **Legibilidade**: nomes significativos, comentários adequados, estrutura clara — facilita manutenção por qualquer pessoa do time.
- **Eficiência**: uso apropriado de memória/processamento; evita redundância, otimiza loops/algoritmos.
- **Confiabilidade**: tratamento de erros, validação de entradas/saídas, testes adequados.
- **Escalabilidade**: suporta aumento de carga/volume sem degradar desempenho.
- **Manutenibilidade**: estrutura modular que permite adicionar recursos/corrigir bugs sem efeitos colaterais em outras partes.
- **Segurança**: prevenção de vulnerabilidades conhecidas, proteção de dados sensíveis, autenticação adequada.

**Sinais de baixa qualidade** (checklist de "code smells" citados): duplicação de código, falta de coesão, complexidade excessiva, falta de documentação, falta de validação de entrada, ausência de testes automatizados, falta de otimização, nomes de variáveis confusos, falta de modularização (blocos monolíticos), condicionais aninhados em excesso, tratamento de exceções ausente/incorreto.

**Como a cultura DevOps sustenta qualidade**: TDD + Integração Contínua garantem que o código seja testado/validado continuamente. Ferramentas de análise estática detectam problemas de segurança, performance e legibilidade automaticamente, incorporadas ao pipeline de CI para dar feedback rápido — antes que o problema cresça. Complementarmente: code review, pair programming, sessões de depuração conjunta.

**Limitações de ferramentas de análise automática** (importante calibrar expectativas): configuração incorreta gera resultados imprecisos; falsos positivos desperdiçam tempo corrigindo não-problemas; falsos negativos deixam passar problemas reais; personalização de regras é limitada; análise pode ser lenta em projetos grandes; regras variam entre versões da ferramenta; e os dados de saída podem ser mal interpretados levando a correções erradas.

**Código legado**: desafios adicionais — dificuldade de integrar a ferramenta a stacks antigas/obsoletas, limitações de detecção, volume de problemas encontrados de uma vez (pode ser avassalador), e restrições de tempo/orçamento para corrigir tudo. Abordagem recomendada: estratégica e gradual — priorizar, alocar recursos, não tentar "resolver tudo de uma vez".

**Ferramentas de mercado citadas**:

| Ferramenta | Escopo |
|---|---|
| **SonarQube / SonarCloud** | Análise estática multi-linguagem — qualidade, vulnerabilidades, conformidade. Suporta Quality Profiles (regras) e Quality Gates (critérios de aprovação/reprovação do pipeline) |
| ESLint | JavaScript — sintaxe e estilo |
| PMD | Multi-linguagem (Java, JS, Ruby, Apex) — duplicação, vulnerabilidades |
| Checkstyle | Java — estilo, convenções, vulnerabilidades |
| CodeSonar | Multi-linguagem (C/C++, Java, JS, Python) — análise estática |
| ReSharper | C#/.NET — extensão para Visual Studio |
| FindBugs | Java — bugs e vulnerabilidades |
| PyLint | Python — sintaxe, estilo, segurança |

O material recomenda validar qualidade **regularmente ao longo de todo o ciclo de vida** do software (não só no fim), tratando isso como investimento que economiza tempo/recursos a longo prazo.

> O `oficina-api` já usa **SonarCloud** integrado à pipeline (ver badges no `README.md` — Quality Gate, Coverage, Bugs, Security Rating, Maintainability Rating) e o step `mvn -B verify sonar:sonar` no workflow de CI existente (ver [[github-actions]]). Isso já cobre a recomendação central desta aula: análise estática automatizada como Quality Gate do pipeline.

## Aplicação no `oficina-api`

### Checklist da pipeline CI/CD obrigatória (Fase 2)

Requisito da fase ([[00-requisitos-fase2]]): *build app → testes → build imagem Docker → deploy cluster K8s → deploy DB → apply manifestos*. Traduzindo os conceitos das 4 aulas para esse fluxo:

- [ ] **Build da aplicação**: `mvn -B verify` (ou `package`) compila e já executa a suíte de testes — evita divergência entre "o que roda local" e "o que roda no CI" (princípio da Aula 3).
- [ ] **Testes automatizados no pipeline**: unitários + integração (Testcontainers, já usado no projeto) rodando a cada push/PR, bloqueando o avanço do pipeline se falharem — não apenas presentes no repo, mas **gatilho obrigatório** antes de build de imagem/deploy.
- [ ] **Build da imagem Docker**: gerado a partir de artefato já testado (nunca antes dos testes passarem) — ver [[dockerizacao]] para o Dockerfile.
- [ ] **Push para registry**: GHCR ou Docker Hub, com tag imutável (ex.: SHA do commit) além de `latest` — rastreabilidade de qual código está em cada imagem.
- [ ] **Deploy do banco de dados** no cluster (manifesto próprio ou provisionado via Terraform) — ver [[terraform]] e [[kubernetes]].
- [ ] **Apply dos manifestos Kubernetes** da aplicação (Deployments, Services, ConfigMaps/Secrets, HPA) — ver [[kubernetes]].
- [ ] **Gate de qualidade**: manter o SonarCloud como etapa do pipeline (já existente) — considerar falha do Quality Gate como bloqueio de merge/deploy, não apenas informativo.
- [ ] **Estratégia de branch**: adotar um workflow simples (GitHub Flow) com PR obrigatório para `main` — CI (build+test+Sonar) deve rodar no PR, deploy (imagem+K8s) apenas a partir de `main`.
- [ ] **Secrets**: credenciais de registry, kubeconfig e tokens (ex. `SONAR_TOKEN`) via GitHub Secrets — nunca hardcoded no workflow ou no repositório.

### Boas práticas de qualidade a manter

- Continuar rodando testes unitários + integração como parte do `verify` do Maven, mantendo paridade entre execução local e CI (Aula 3).
- Preservar os 5 badges do SonarCloud no README como sinal visível de saúde do código (Quality Gate, Coverage, Bugs, Security Rating, Maintainability Rating).
- Tratar falso positivo/negativo do SonarCloud com julgamento — não desabilitar regras às cegas nem ignorar achados sem análise (limitação conhecida das ferramentas de análise estática, Aula 4).
- Usar PR + code review como ponto de controle de qualidade adicional ao gate automatizado — a aula de GitHub reforça PR como etapa obrigatória, não apenas formalidade.
- Evitar os "code smells" listados (duplicação, falta de coesão, complexidade excessiva, falta de tratamento de exceção) especialmente ao refatorar o código herdado da Fase 1 — que se enquadra no cenário de "código legado" discutido na Aula 4: priorizar e corrigir gradualmente, não tentar reescrever tudo de uma vez.

## Ver também

- [[github-actions]] — sintaxe, jobs, runners e esqueleto de pipeline CI/CD completo para o `oficina-api`.
- [[dockerizacao]] — Dockerfile, docker-compose e estratégia de imagem.
- [[kubernetes]] — manifestos em `/k8s` (Deployments, Services, ConfigMaps/Secrets, HPA).
- [[terraform]] — provisionamento do cluster e do banco em `/infra`.
- [[00-requisitos-fase2]] — escopo obrigatório completo da Fase 2.
