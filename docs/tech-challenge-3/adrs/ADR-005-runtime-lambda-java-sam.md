# ADR-005 — Lambda em Java 21 com AWS SAM

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

A function de autenticação precisa de um runtime. A Aula 06 de Serverless faz exatamente este exercício — criar uma Lambda **em Java** com o **AWS SAM CLI**, rodar localmente para depuração e publicar integrada ao API Gateway. A aplicação principal é Java 21 / Spring Boot 3.4.7, e o time trabalha nessa stack.

Contrapeso vem da Aula 02 de Serverless: o Lambda cobra por **tempo de execução**, arredondado ao 1 ms mais próximo, e o custo escala com a memória configurada. A conclusão literal da aula é que *"Lambdas devem ser simples e o mais otimizados possível, para que executem rápido, pois refletirá no custo final"*. Cold start de JVM contraria isso.

## Decisão

**Java 21** com **AWS SAM**, sem SnapStart no primeiro momento.

Configuração inicial: 512 MB de memória, timeout de 10 s, arquitetura `arm64`.

**Gatilho de revisão definido de antemão:** medir p95 do cold start na Fase F2. Se ultrapassar **3 segundos**, ativar SnapStart — o que exige publicar versão e alias da função e apontar o API Gateway para o alias.

## Alternativas consideradas

**Node.js 22.** Cold start na casa de 200 ms, driver `pg` leve, pacote pequeno. Tecnicamente a melhor escolha para uma função que faz uma consulta e assina um token. Descartada por aderência: sairia da stack do time e do que a aula demonstrou, e introduziria uma segunda linguagem no projeto por causa de uma função de ~200 linhas.

**Java 21 + SnapStart desde o início.** Derrubaria o cold start para ~300 ms mantendo a stack. Descartada como partida por adicionar versionamento e alias antes de haver evidência de que o problema existe — otimização sem medição. Fica como resposta pronta ao gatilho acima.

**GraalVM native image.** Cold start mínimo, mas exige build nativo e reflete mal com bibliotecas que usam reflection (JJWT, driver JDBC). Complexidade desproporcional.

## Consequências

**Positivas**
- Mesma linguagem, mesmas ferramentas, mesmo `pom.xml` mental do time.
- **Reuso direto de `CpfCnpj`** (`domain/valueobject/CpfCnpj.java`), que já implementa validação de CPF — a Lambda não reimplementa a regra, evitando divergência entre os dois validadores.
- SAM CLI permite `sam local invoke` com Docker, encurtando o ciclo de depuração sem deploy.
- `arm64` é mais barato que x86 na tabela do Lambda.

**Negativas**
- **Cold start de 1 a 3 s** na primeira invocação e após períodos ociosos. Numa demo ao vivo, a primeira chamada parece travada.
- Pacote maior (JAR com JJWT + driver JDBC), o que aumenta o tempo de inicialização.
- **Acoplamento à AWS pelo SAM**, alerta explícito da Aula 06: migrar de provedor acarreta reconstruções.
- Reusar `CpfCnpj` exige publicar a classe como artefato compartilhado ou duplicá-la — decisão detalhada em [SPEC-01](../../../../artefatos-tech-challenge-3/plano-implementacao/specs/SPEC-01-lambda-auth-cpf.md) §4.
