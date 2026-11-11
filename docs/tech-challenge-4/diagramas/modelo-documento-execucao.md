# Modelo de documento — Execution Service (MongoDB / DocumentDB)

Um documento por OS na coleção `execucoes` (`_id` = id da OS). Etapas, apontamentos e histórico ficam embutidos: o agregado é lido e gravado inteiro, e a atualização de um documento é atômica.

```mermaid
classDiagram
    class Execucao {
        _id : osId
        sagaId
        clienteId
        status
        etapaAtual
        prioridade : int
        enfileiradaEm : Instant
        observacoesDiagnostico
        motivoFalha
        versao : long
    }
    class Veiculo {
        placa
        marca
        modelo
        anoFabricacao
    }
    class Etapa {
        tipo : DIAGNOSTICO|REPARO
        mecanicoId
        inicio
        fim
    }
    class Apontamento {
        mecanicoId
        texto
        registradoEm
    }
    class MudancaStatus {
        de
        para
        em
        motivo
    }
    class ItemReparo {
        tipo : SERVICO|PECA
        descricao
        quantidade
    }
    Execucao *-- Veiculo
    Execucao *-- "0..*" Etapa
    Execucao *-- "0..*" Apontamento
    Execucao *-- "0..*" MudancaStatus
    Execucao *-- "0..*" ItemReparo
```

Índices criados na subida da aplicação:

| Coleção | Índice | Uso |
|---|---|---|
| `execucoes` | `{status: 1, prioridade: -1, enfileiradaEm: 1}` | fila por etapa |
| `execucoes` | `{clienteId: 1}` | visão do cliente |
| `outbox` | `{status: 1, criadaEm: 1}` | relay |
| `mensagensProcessadas` | `{processadaEm: 1}`, TTL 14 dias | inbox |
