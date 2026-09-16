# ADR-005 — Lambda em Java 21 com AWS SAM

- **Data:** 04/09/2026
- **Status:** Aceita

## Contexto

A function de autenticação precisa de um runtime. A Aula 06 de Serverless apresenta este exercício: criar uma Lambda em Java com o AWS SAM CLI, executá-la localmente para depuração e publicá-la integrada ao API Gateway. A aplicação principal é Java 21 / Spring Boot 3.4.7, e o time trabalha nessa stack.

Em sentido oposto, a Aula 02 de Serverless indica que o Lambda cobra por tempo de execução, arredondado ao 1 ms mais próximo, e que o custo escala com a memória configurada. A aula conclui que *"Lambdas devem ser simples e o mais otimizados possível, para que executem rápido, pois refletirá no custo final"*. O cold start da JVM vai contra essa recomendação.

## Decisão

Java 21 com AWS SAM, sem SnapStart no primeiro momento.

Configuração inicial: 512 MB de memória, timeout de 10 s, arquitetura `arm64`.

**Gatilho de revisão definido previamente:** medir o p95 do cold start na Fase F2. Se ultrapassar 3 segundos, ativar SnapStart, o que exige publicar versão e alias da função e apontar o API Gateway para o alias.

## Alternativas consideradas

**Node.js 22.** Cold start na casa de 200 ms, driver `pg` leve e pacote pequeno. Tecnicamente, é a melhor escolha para uma função que faz uma consulta e assina um token. Descartada por aderência: sairia da stack do time e do que a aula demonstrou, e introduziria uma segunda linguagem no projeto por causa de uma função de ~200 linhas.

**Java 21 + SnapStart desde o início.** Reduziria o cold start para ~300 ms mantendo a stack. Descartada como ponto de partida por adicionar versionamento e alias antes de haver evidência do problema, o que configuraria otimização sem medição. Fica como resposta prevista para o gatilho acima.

**GraalVM native image.** Cold start mínimo, mas exige build nativo e tem compatibilidade limitada com bibliotecas que usam reflection (JJWT, driver JDBC). Complexidade desproporcional.

## Consequências

**Positivas**
- Mesma linguagem, ferramentas e estrutura de projeto (`pom.xml`) já conhecidas pelo time.
- **Reuso direto de `CpfCnpj`** (`domain/valueobject/CpfCnpj.java`), que já implementa a validação de CPF. A Lambda não reimplementa a regra, o que evita divergência entre os dois validadores.
- O SAM CLI permite `sam local invoke` com Docker, encurtando o ciclo de depuração sem deploy.
- `arm64` é mais barato que x86 na tabela de preços do Lambda.

**Negativas**
- **Cold start de 1 a 3 s** na primeira invocação e após períodos ociosos. Em uma demonstração ao vivo, a primeira chamada pode aparentar travamento.
- Pacote maior (JAR com JJWT + driver JDBC), o que aumenta o tempo de inicialização.
- **Acoplamento à AWS pelo SAM.** A Aula 06 alerta explicitamente que migrar de provedor acarreta reconstruções.
- Reusar `CpfCnpj` exige publicar a classe como artefato compartilhado ou duplicá-la; a decisão está detalhada em [SPEC-01](../../../../artefatos-tech-challenge-3/plano-implementacao/specs/SPEC-01-lambda-auth-cpf.md) §4.
