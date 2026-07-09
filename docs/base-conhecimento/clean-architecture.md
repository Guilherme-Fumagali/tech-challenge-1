# Clean Architecture

> Fonte: Aulas 1-8, disciplina *Software Architecture* (Fase 2, PosTech FIAP), professor Erick Muller (aulas 1, 2, 5, 6, 7, 8 — conteúdo de Clean/Hexagonal Architecture) e Fabiano da Silva Carneiro (aulas 3 e 4 — pertencem ao módulo "DevOps (IaC and CI/CD)" mas vieram no mesmo material; tratam de testes automatizados/CI e qualidade de código-fonte, incluídas aqui por serem diretamente relevantes ao requisito de testes da Fase 2). Ver também [[00-requisitos-fase2]].

## Conceitos-chave

### SOLID (base de tudo — aulas 1 e 8)

| Princípio | Definição das aulas | Ideia prática |
|---|---|---|
| **S** — Single Responsibility | Uma unidade de código deve ter uma única responsabilidade e um único motivo para mudar. | Não deve alterar comportamento "dependendo do tipo de chamada"; se faz mais de uma coisa, quebrar. |
| **O** — Open-Closed | Unidades de código devem ser abertas para extensão, fechadas para alteração. | Nova funcionalidade = novo componente/extensão, não editar o que já existe e já funciona (evita segregar responsabilidade com `if`/`switch` crescente). |
| **L** — Liskov Substitution | Uma classe derivada deve poder substituir a classe base sem quebrar o software. | Implementado via interfaces; garante que quem depende da abstração não dependa de comportamento específico de uma implementação concreta. |
| **I** — Interface Segregation | Uma classe não deve implementar métodos/interfaces que não usa. | Interfaces enxutas e focadas; dependências definidas pelo comportamento necessário, não pela implementação concreta (analogia: tomada elétrica — importa a interface, não a instalação por trás). |
| **D** — Dependency Inversion | Módulo de alto nível não depende de módulo de baixo nível; ambos dependem de abstrações. | Não confundir com injeção de dependência: inversão é o **conceito** (depender de abstração), injeção é o **mecanismo** (como a implementação concreta chega até quem usa). |

O SOLID é a base para as regras de organização em camadas que vêm a seguir: SOLID diz como cada unidade deve ser construída; Clean Architecture diz como organizar e conectar essas unidades.

### As camadas (aulas 2, 5, 8) — "cebola", não "bolo"

Diagrama de referência (Martin, 2019), do centro para fora:

1. **Entities (Entidades)** — núcleo. Modelam os conceitos de negócio (ex.: `Estudante`, `Venda`, `Cliente`) na **linguagem ubíqua do domínio** (ver [[linguagem_ubiqua]]), não na linguagem de implementação. Carregam dados **e** regras/invariantes de consistência (ex.: "não existe Matrícula sem Estudante"). Devem ser testáveis isoladamente.
2. **Use Cases (Casos de Uso)** — implementam as regras de negócio da aplicação, uma funcionalidade por componente (ex.: `MatricularAluno`, `EfetuarMovimentacaoFinanceira`). Só podem depender de Entidades e de outros casos de uso — nunca de banco de dados, API ou UI. Recebem dados já formatados vindos de fora (via injeção) e devolvem resultado, sem saber como o resultado chegará ao mundo exterior.
3. **Interface Adapters (Adaptadores de Interface)** — ponte entre o núcleo e o mundo exterior. Três tipos de componente:
   - **Controller**: recebe a chamada externa, converte dados de entrada em algo que o caso de uso entende, executa o caso de uso, encaminha o resultado. Não sabe "sucesso" ou "erro" de negócio — é um mensageiro.
   - **Presenter**: formata a saída no protocolo/formato que o cliente espera (ex.: JSON com headers HTTP corretos). Normalmente instanciado pelo Controller.
   - **Gateway**: abstrai o acesso a um recurso externo (BD, API) expondo uma operação no nível de abstração que o caso de uso entende (ex.: `EstudanteGateway.obterEstudantePorPessoa(...)`), nunca uma função de baixo nível do driver.
4. **Frameworks & Drivers** — camada mais externa: servidor HTTP, rotas de API, ORM/driver de banco, autenticação/autorização, UI. É "visível" mas não é o núcleo — comparável a teclado/tela de um computador: essencial para operar, irrelevante para a lógica interna.

**Regras de dependência** (aula 8, explícitas):
- Componentes internos **nunca** acessam/dependem diretamente de componentes externos; se precisarem, recebem via **injeção de dependência** através de uma interface definida internamente.
- Componentes da **mesma camada** podem depender e interagir livremente entre si.

### Clean Architecture vs. Arquitetura Hexagonal (aula 2)

Não são a mesma coisa, mas compartilham a ideia central (Entidades no centro, mundo externo tratado via Portas/Adaptadores). Diferença citada: Clean Architecture divide as camadas externas às Entidades/Casos de Uso em **duas** camadas (Interface Adapters e Frameworks & Drivers), enquanto o desenho hexagonal fala em termos de Ports (Input/Output) e Adapters de forma mais unificada.

### Mundo externo como "detalhe" (aula 7)

Ideia central: repositórios de dados, protocolo de acesso (REST, socket, eventos) e o framework usado são **detalhes de implementação**, não decisões estruturais do núcleo.
- **Repositórios de dados**: chamar de "repositório" (não "banco de dados") já reforça que a origem/destino dos dados é intercambiável; o acesso é isolado atrás de um Gateway.
- **Acesso à aplicação**: o software deve funcionar independente de como é acessado (web, mobile, desktop); acoplar regra de negócio a um único tipo de entrada é um erro que trava o software num único modo de uso.
- **Backend-for-Frontend (BFF)**: técnica citada — criar endpoints/APIs segregados por tipo de cliente (mobile, browser) em vez de sobrecarregar um único endpoint genérico com lógica condicional de formatação.
- **Frameworks**: duas estratégias possíveis — (a) usar micro-frameworks mantendo baixo acoplamento; (b) usar framework robusto mas isolando o que ele fornece (ex.: ORM) atrás de Gateways/camada de dados, mantendo casos de uso isolados dos Controllers.

### Componentização (aula 6)

- **Componente**: unidade de código com responsabilidade bem definida e interface de uso clara e explícita (nomes de operação correspondem à operação).
- **Arquitetura monolítica**: centraliza tudo numa unidade só. Não é "errada" por definição (ok para soluções pequenas/embarcadas), mas se torna problema quando cresce sem separação de responsabilidades — custo de manutenção sobe com o tempo.
- **Regra da responsabilidade mínima**: um componente deve implementar o mínimo necessário para ser útil; o que sobra deve ser segregado em outro componente (não implementar "pela metade").
- **Composição e desacoplamento**: componentes podem ser internamente acoplados mas expostos de forma desacoplada ao resto do sistema (ex.: um componente de acesso a dados composto por driver de BD + entidades + adaptador de tradução).
- **Reuso**: bibliotecas, frameworks e pacotes de terceiros (npm, pip, Maven) existem para não reconstruir componentes já testados; usar o mínimo necessário, não usar "porque existe".
- **Antipadrão citado explicitamente**: adaptar a arquitetura ao framework/ferramenta escolhida antes de entender o problema — gera "arquitetura entortada" e débito técnico. O caminho correto é desenhar a arquitetura a partir do problema e só depois escolher os componentes/frameworks.

### Testes automatizados e CI (aula 3)

- Integração contínua não é só "entregar sempre" — é aumentar qualidade via testes e verificações constantes que reduzem o custo de corrigir bugs tardiamente.
- Distinção: **teste automático** (executado sob estímulo humano, ex. rodar localmente) vs. **teste contínuo** (disparado automaticamente a cada mudança no repositório, via pipeline).
- TDD citado como prática central: escrever teste que falha, implementar até passar, refatorar.
- Pipeline de teste deve rodar exatamente os mesmos comandos que a pessoa desenvolvedora rodaria localmente (`mvn verify`, `npm test`, `pytest`, etc.) — nunca um processo manual paralelo.
- Estratégia recomendada: testar tudo o que for possível, do unitário (mais barato, mais próximo do CI) ao funcional (mais caro, mais próximo de homologação/produção).

### Qualidade de código-fonte (aula 4)

Características que definem qualidade (usadas como checklist): **legibilidade, eficiência, confiabilidade, escalabilidade, manutenibilidade, segurança**.

Sinais explícitos de baixa qualidade (aula cita como lista): duplicação de código, falta de coesão, complexidade excessiva, falta de documentação, falta de validação de entrada, ausência de testes automatizados, falta de otimização, nomes de variáveis confusos, falta de modularização, condicionais aninhados excessivos, falta de tratamento de exceções.

Ferramentas de análise estática citadas: SonarQube/SonarCloud, ESLint, PMD, Checkstyle, CodeSonar, ReSharper, FindBugs, PyLint. Integradas ao pipeline de CI para dar feedback antes que o problema cresça. Limitações citadas: configuração incorreta, falso positivo/negativo, personalização limitada, tempo de execução em bases grandes, dependência de versão.

## Boas práticas

- Nomear e modelar Entidades e Casos de Uso na linguagem do domínio (linguagem ubíqua), não em termos técnicos.
- Um caso de uso = uma funcionalidade; casos de uso só dependem de Entidades e de outros casos de uso.
- Nunca deixar código de acesso a banco/API/framework vazar para dentro de Entities ou Use Cases — isso é responsabilidade de Gateways/Frameworks & Drivers.
- Definir interfaces (portas) nas camadas internas e implementá-las nas camadas externas — quem define o "contrato" é o código que precisa da dependência, não quem a fornece (Dependency Inversion na prática).
- Controllers finos: apenas coordenam (parse de entrada → chamar caso de uso → repassar saída), sem lógica de negócio.
- Isolar formatação de resposta (Presenter) de execução de regra (Controller/Use Case).
- Preferir muitos componentes pequenos e especializados a poucos componentes genéricos — mesmo que isso gere mais classes/arquivos.
- Escolher framework/tecnologia depois de entender o problema, não antes.
- Tratar toda origem/destino de dados e todo canal de acesso (REST, fila, etc.) como detalhe substituível.
- Rodar testes automatizados (unitários no mínimo) via pipeline de CI, com os mesmos comandos usados localmente (`mvn verify`).
- Usar análise estática (SonarQube etc.) como gate de qualidade contínuo, não como auditoria pontual.

## Armadilhas comuns / anti-patterns

- **Camada de Casos de Uso com código de infraestrutura** (ex.: abrir conexão de banco dentro de um Use Case) — quebra o isolamento que é a razão de ser da Clean Architecture.
- **Componentes genéricos "faz-tudo"** — violam SRP e tendem a virar um monólito disfarçado de várias classes.
- **Adaptar a arquitetura ao framework** em vez de adaptar o framework à arquitetura — sintoma: "entortar" o desenho para caber numa ferramenta escolhida cedo demais.
- **Acoplar regra de negócio a um único canal de acesso** (ex.: só funciona via REST síncrono) — impede reuso por outros clientes sem duplicar/forçar a regra de negócio.
- **Controller decidindo regra de negócio** (ex.: formatar resposta diferente por tipo de cliente dentro do próprio código de negócio) — deveria estar isolado em endpoints/Presenters diferentes (BFF).
- **Testes ausentes tratados como "processo manual documentado"** — a aula é explícita: isso não é aconselhável hoje; testes devem ser automatizados e contínuos.
- **Confundir inversão de dependência com injeção de dependência** — a primeira é o princípio (depender de abstração), a segunda é apenas o mecanismo.
- **Ignorar falso positivo/negativo de ferramentas de análise estática** sem calibrar o Quality Gate — gera ruído ou falsa sensação de segurança.

## Aplicação no `oficina-api`

O projeto já segue a estrutura de camadas ensinada nas aulas, com correspondência direta:

| Camada da Clean Architecture | Pasta no `oficina-api` | Exemplos encontrados |
|---|---|---|
| Entities | `domain/entity`, `domain/valueobject` | `OrdemServico`, `Cliente`, `Veiculo`, `Peca`, `Servico`, `StatusOS`, `CpfCnpj`, `Placa` |
| Regras/invariantes de negócio | `domain/exception` | `TransicaoInvalidaException`, `EstoqueInsuficienteException`, `RecursoNaoEncontradoException` |
| Portas (interfaces definidas pelo núcleo) | `domain/repository`, `application/port` | `OrdemServicoRepository`, `ClienteRepository`, `NotificacaoService` |
| Use Cases | `application/usecase/*` | `CriarOrdemServicoUseCase`, `IniciarDiagnosticoUseCase`, `GerarOrcamentoUseCase`, `AprovarOrcamentoUseCase`/`ReprovarOrcamentoUseCase`, `ConcluirServicosUseCase`, `EntregarVeiculoUseCase`, `ConsultarStatusOSUseCase` |
| Gateways / Adapters (implementação das portas) | `infrastructure/persistence/adapter` | `OrdemServicoRepositoryAdapter`, `ClienteRepositoryAdapter`, etc. (implementam as interfaces de `domain/repository`) |
| Controllers | `infrastructure/web/controller` | `OrdemServicoController`, `OrdemServicoCicloVidaController`, `AuthController` |
| Presenters (implícito) | `infrastructure/web/dto/response` | DTOs de resposta — hoje montados dentro do próprio Controller; avaliar se vale extrair um Presenter dedicado quando a formatação ficar mais complexa |
| Frameworks & Drivers | `infrastructure/config`, `infrastructure/security`, `infrastructure/persistence/repository` (Spring Data/JPA) | Configuração Spring, filtros JWT, repositórios JPA |

O mapeamento confirma que o projeto já pratica a **Dependency Rule**: `domain/repository` define a interface (porta) e `infrastructure/persistence/adapter` a implementa — é o Dependency Inversion Principle sendo aplicado exatamente como descrito nas aulas 1 e 8 (módulo de alto nível, o caso de uso, não depende de JPA/Postgres diretamente).

### Pontos a revisar/reforçar na Fase 2 (refatoração exigida)

1. **Auditoria de vazamento de camada** — conferir se nenhuma classe em `domain/` ou `application/usecase/` importa algo de `infrastructure/` (Spring, JPA, `jakarta.persistence`, etc.). Isso violaria a regra "componentes internos não dependem de componentes externos" (aula 8). Um teste de arquitetura (ex. ArchUnit) é uma forma automatizada de garantir isso continuamente, alinhado com a cultura de CI/qualidade das aulas 3-4.
2. **Controllers finos** — revisar `OrdemServicoController` e `OrdemServicoCicloVidaController` para garantir que só orquestram (bind de request → use case → resposta), sem regra de negócio (ex. cálculo de orçamento, transição de status) vazando para o controller.
3. **Presenters implícitos** — hoje a formatação de resposta parece estar dentro do controller/DTO; está aceitável para o tamanho atual do projeto, mas se a lógica de apresentação crescer (ex. formatos diferentes por client), considerar extrair um Presenter, seguindo o padrão BFF descrito na aula 7 (relevante se surgir necessidade de endpoints diferentes por tipo de consumidor).
4. **Cobertura de testes em fluxos críticos** (exigência explícita da Fase 2, ver [[00-requisitos-fase2]]) — hoje há testes em `domain` (`OrdemServicoTest`, `PecaTest`, `CpfCnpjTest`, `PlacaTest`, `ExceptionTest`) e `application` (um teste por use case relevante). Reforçar especificamente:
   - Transições de status da OS (`Recebida → Diagnóstico → Aguardando Aprovação → Execução → Finalizada → Entregue`) e transições inválidas (`TransicaoInvalidaException`).
   - Fluxo de aprovação/reprovação de orçamento (endpoint de notificação externa).
   - Ordenação da listagem de OS por status/antiguidade e exclusão lógica de OS finalizadas/entregues.
   - Testes de integração cobrindo Controller → Use Case → Adapter (ex. `@SpringBootTest` ou slice tests), não só unitários isolados — alinhado com a "pirâmide de testes" discutida na aula 3.
5. **Qualidade de código contínua** (aula 4) — integrar SonarQube/SonarCloud (ou equivalente) na pipeline de CI/CD da Fase 2 como Quality Gate, cobrindo ao menos duplicação, cobertura de testes e vulnerabilidades básicas. Ver [[github-actions]] / [[devops]] para a esteira.
6. **Componentização dos casos de uso de Ordem de Serviço** — há vários use cases pequenos e especializados (`AdicionarPecaAOSUseCase`, `AdicionarServicoAOSUseCase`, `GerarOrcamentoUseCase`, etc.), o que está alinhado com a regra de responsabilidade mínima (aula 6); manter esse padrão ao adicionar novas funcionalidades da Fase 2 em vez de inchar um único use case "OrdemServicoUseCase" genérico.
7. **Independência de detalhe de infraestrutura** — ao introduzir Docker/Kubernetes/Terraform na Fase 2 (ver [[dockerizacao]], [[kubernetes]], [[terraform]]), lembrar que a Clean Architecture já deveria tornar essa mudança de ambiente de execução transparente para `domain/` e `application/`: se a troca de infraestrutura exigir alterar regra de negócio, é sinal de vazamento de camada a corrigir primeiro.

## Referências citadas nas aulas

- MARTIN, R. *Arquitetura Limpa*. Rio de Janeiro: Alta Books, 2019.
- MARTIN, R. *Clean Code: A Handbook of Agile Software Craftsmanship*. Pearson, 2008.
- MARTIN, R. *The Clean Coder: A Code of Conduct for Professional Programmers*. Pearson, 2011.
- EVANS, E. *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley, 2003.
- DUVALL, P.; MATYAS, S.; GLOVER, A. *Continuous Integration: Improving Software Quality and Reducing Risk*. Addison-Wesley, 2007.
