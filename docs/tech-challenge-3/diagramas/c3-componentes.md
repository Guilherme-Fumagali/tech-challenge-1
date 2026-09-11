# C4 · Nível 3 — Componentes da aplicação

Arquitetura interna do contêiner `oficina-api`, em Clean Architecture.

```mermaid
C4Component
    title oficina-api — Componentes

    Container_Boundary(app, "oficina-api") {
        Component(controller, "Controllers", "Spring MVC", "ClienteController, OrdemServicoController, OrdemServicoCicloVidaController, aprovação externa.")
        Component(filter, "JwtAuthenticationFilter", "Spring Security", "Valida o JWT emitido pela Lambda e popula o SecurityContext a partir das claims.")
        Component(handler, "GlobalExceptionHandler", "@RestControllerAdvice", "Traduz exceções de domínio em ProblemDetail e registra falha de transição como métrica.")
        Component(usecase, "Casos de uso", "POJOs", "CriarOrdemServico, IniciarDiagnostico, GerarOrcamento, Aprovar, Concluir, Entregar.")
        Component(domain, "Domínio", "Entidades e VOs", "OrdemServico, Cliente, StatusOS, CpfCnpj. Máquina de transição de status.")
        Component(port, "Portas", "Interfaces", "OrdemServicoRepository, NotificacaoService, MetricasOrdemServico.")
        Component(adapter, "Adaptadores de persistência", "Spring Data JPA + MapStruct", "Drena os eventos de transição da OS e publica as métricas de negócio.")
        Component(metrics, "MicrometerMetricasOrdemServico", "Micrometer", "oficina.os.abertas, duracao_status, transicoes, integracao.falhas.")
        Component(notif, "SmtpNotificacaoService", "Spring Mail", "E-mail de orçamento com links de aprovação.")
    }

    ContainerDb(rds, "RDS PostgreSQL", "db.t4g.micro", "")
    System_Ext(nr, "New Relic", "OTLP")

    Rel(filter, controller, "Autentica e encaminha")
    Rel(controller, usecase, "Executa")
    Rel(controller, handler, "Exceções")
    Rel(handler, metrics, "registrarFalhaTransicao")
    Rel(usecase, domain, "Opera o agregado")
    Rel(usecase, port, "Depende de abstração")
    Rel(adapter, port, "Implementa")
    Rel(adapter, metrics, "Drena TransicaoOS e publica")
    Rel(adapter, rds, "JDBC")
    Rel(notif, port, "Implementa NotificacaoService")
    Rel(metrics, nr, "Métricas de negócio", "OTLP")
```

## A decisão que sustenta as métricas de negócio

Os três dashboards exigidos precisam de métricas que o agente de APM não conhece — ele entrega latência e erro HTTP, mas não sabe o que é uma ordem de serviço.

A alternativa óbvia seria injetar `MeterRegistry` em cada caso de uso, mudando **nove construtores** e o `UseCaseConfig`. Em vez disso:

1. `OrdemServico` acumula eventos `TransicaoOS` num método único de transição, que já valida a mudança de status e mede a permanência no status anterior.
2. O adaptador de persistência **drena** esses eventos em `salvar()` — o funil por onde toda transição passa.

O resultado é que a métrica descreve **evento de negócio**, não requisição HTTP: se amanhã a OS for aberta por outro canal, a contagem continua correta. E o domínio não conhece Micrometer.
