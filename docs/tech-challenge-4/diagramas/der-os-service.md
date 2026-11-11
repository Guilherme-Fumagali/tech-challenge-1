# DER — banco do OS Service (PostgreSQL)

Tabelas do domínio herdadas do monólito e tabelas novas da saga e da mensageria. As referências a cliente, veículo, serviço e peça continuam com chave estrangeira porque estão no mesmo serviço; nenhuma tabela referencia dado de outro serviço.

```mermaid
---
config:
  layout: elk
---
erDiagram
    clientes ||--o{ veiculos : possui
    clientes ||--o{ ordens_servico : solicita
    veiculos ||--o{ ordens_servico : atendido_em
    ordens_servico ||--o{ itens_servico : contem
    ordens_servico ||--o{ itens_peca : contem
    servicos ||--o{ itens_servico : referenciado
    pecas ||--o{ itens_peca : reservada
    ordens_servico ||--|| sagas_ordem_servico : orquestrada_por
    sagas_ordem_servico ||--o{ saga_passos : registra
    ordens_servico ||--o{ historico_status_os : historico

    clientes {
        uuid id PK
        varchar cpf_cnpj UK
        varchar nome
        varchar email
        varchar status
    }
    veiculos {
        uuid id PK
        varchar placa UK
        varchar marca
        varchar modelo
        int ano_fabricacao
        uuid cliente_id FK
    }
    funcionarios {
        uuid id PK
        varchar cpf UK
        varchar nome
        varchar email
        varchar status
    }
    servicos {
        uuid id PK
        varchar nome
        numeric preco_unitario
        numeric tempo_estimado_horas
    }
    pecas {
        uuid id PK
        varchar nome
        numeric preco_unitario
        int quantidade_estoque "CHECK >= 0"
        int estoque_minimo
        bigint versao "lock otimista"
    }
    ordens_servico {
        uuid id PK
        uuid cliente_id FK
        uuid veiculo_id FK
        varchar status
        timestamp data_abertura
        timestamp data_aprovacao
        timestamp data_inicio
        timestamp data_conclusao
        bigint versao "lock otimista"
    }
    itens_servico {
        uuid id PK
        uuid ordem_servico_id FK
        uuid servico_id FK
        numeric preco_snapshot
        int quantidade
    }
    itens_peca {
        uuid id PK
        uuid ordem_servico_id FK
        uuid peca_id FK
        numeric preco_snapshot
        int quantidade
    }
    sagas_ordem_servico {
        uuid id PK
        uuid os_id FK,UK
        varchar estado
        varchar motivo_compensacao
        varchar compensacoes_pendentes
        varchar ultimo_comando_tipo
        timestamp prazo_resposta
        int tentativas_reenvio
        bigint versao
    }
    saga_passos {
        uuid id PK
        uuid saga_id FK
        int sequencia
        varchar direcao
        varchar tipo
        uuid mensagem_id
        varchar resultado
        timestamp ocorrido_em
    }
    historico_status_os {
        uuid id PK
        uuid os_id FK
        varchar status_origem
        varchar status_destino
        varchar evento
        timestamp ocorrido_em
    }
    outbox {
        uuid id PK "mensagemId"
        varchar fila
        varchar tipo
        jsonb payload
        jsonb cabecalhos
        timestamp publicada_em
    }
    mensagens_processadas {
        uuid mensagem_id PK
        varchar tipo
        timestamp processada_em
    }
```
