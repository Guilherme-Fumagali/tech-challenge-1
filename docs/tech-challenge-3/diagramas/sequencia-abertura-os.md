# Diagrama de sequência — Abertura de ordem de serviço

Requisito literal do enunciado. Mostra o caminho completo, incluindo a emissão das métricas de negócio.

```mermaid
sequenceDiagram
    autonumber
    actor A as Atendente
    participant GW as API Gateway
    participant AZ as Lambda authorizer
    participant CT as OrdemServicoController
    participant UC as CriarOrdemServicoUseCase
    participant CR as ClienteRepository
    participant VR as VeiculoRepository
    participant OS as OrdemServico (agregado)
    participant RA as OrdemServicoRepositoryAdapter
    participant MT as MicrometerMetricas
    participant DB as PostgreSQL
    participant NR as New Relic

    A->>GW: POST /api/ordens { clienteId, veiculoId }
    GW->>AZ: valida JWT
    AZ-->>GW: isAuthorized = true
    GW->>CT: VPC Link → NLB → NodePort

    CT->>CT: JwtAuthenticationFilter popula SecurityContext e MDC(cliente.id)
    CT->>UC: executar(clienteId, veiculoId)

    UC->>CR: buscarPorId(clienteId)
    CR-->>UC: Cliente

    UC->>VR: buscarPorId(veiculoId)
    VR-->>UC: Veiculo

    alt veículo não pertence ao cliente
        UC-->>A: 400 DomainException
    end

    UC->>OS: new OrdemServico(id, clienteId, veiculoId)
    Note right of OS: nasce em RECEBIDA<br/>marca novaOrdem = true<br/>dataUltimaTransicao = agora

    UC->>RA: salvar(os)
    RA->>DB: INSERT ordens_servico
    RA->>OS: consumirMarcaDeNovaOrdem()
    OS-->>RA: true
    RA->>MT: registrarAbertura()
    MT->>NR: oficina.os.abertas +1 (OTLP)

    RA-->>UC: OrdemServico
    UC-->>CT: OrdemServico
    CT-->>A: 201 Created + id da OS

    Note over A,NR: Transição posterior — ex.: iniciar diagnóstico

    A->>CT: PATCH /api/ordens/{id}/diagnostico
    CT->>OS: iniciarDiagnostico()
    OS->>OS: transicionarPara(EM_DIAGNOSTICO)
    Note right of OS: valida transição<br/>mede permanência em RECEBIDA<br/>enfileira TransicaoOS
    CT->>RA: salvar(os)
    RA->>DB: UPDATE
    RA->>OS: drenarTransicoes()
    OS-->>RA: [TransicaoOS(RECEBIDA → EM_DIAGNOSTICO, 4h12m)]
    RA->>MT: registrarTransicao(...)
    MT->>NR: oficina.os.transicoes +1<br/>oficina.os.duracao_status = 4h12m
```

## Por que o adaptador publica a métrica

Toda transição termina em `repository.salvar(os)` — é o funil natural. Instrumentar ali evita injetar dependência de métrica em nove casos de uso, e mantém o domínio sem conhecer Micrometer.

O agregado acumula os eventos; a fronteira de persistência os drena e publica. É evento de domínio na versão mínima, sem barramento nem publisher.
