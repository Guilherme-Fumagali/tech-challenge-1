-- ============================================================
-- V1 — Schema inicial: Oficina Mecânica
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Clientes
CREATE TABLE clientes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cpf_cnpj    VARCHAR(14) NOT NULL UNIQUE,
    nome        VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    telefone    VARCHAR(20)
);

-- Veículos
CREATE TABLE veiculos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    placa           VARCHAR(8)  NOT NULL UNIQUE,
    marca           VARCHAR(100) NOT NULL,
    modelo          VARCHAR(100) NOT NULL,
    ano_fabricacao  INT NOT NULL,
    cliente_id      UUID NOT NULL REFERENCES clientes(id)
);

-- Catálogo de Serviços
CREATE TABLE servicos (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                  VARCHAR(255) NOT NULL,
    descricao             TEXT,
    preco_unitario        NUMERIC(10, 2) NOT NULL,
    tempo_estimado_horas  NUMERIC(5, 2)
);

-- Catálogo de Peças / Insumos
CREATE TABLE pecas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome                VARCHAR(255) NOT NULL,
    descricao           TEXT,
    preco_unitario      NUMERIC(10, 2) NOT NULL,
    quantidade_estoque  INT NOT NULL DEFAULT 0,
    estoque_minimo      INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_estoque_nao_negativo CHECK (quantidade_estoque >= 0)
);

-- Ordens de Serviço
CREATE TABLE ordens_servico (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id      UUID NOT NULL REFERENCES clientes(id),
    veiculo_id      UUID NOT NULL REFERENCES veiculos(id),
    status          VARCHAR(30) NOT NULL,
    data_abertura   TIMESTAMP NOT NULL DEFAULT NOW(),
    data_inicio     TIMESTAMP,
    data_conclusao  TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN (
        'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO',
        'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE', 'CANCELADA'
    ))
);

-- Itens de Serviço (dentro de uma OS) — com Snapshot de Preço
CREATE TABLE itens_servico (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id) ON DELETE CASCADE,
    servico_id       UUID NOT NULL REFERENCES servicos(id),
    nome_servico     VARCHAR(255) NOT NULL,
    preco_snapshot   NUMERIC(10, 2) NOT NULL,
    quantidade       INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_qtd_servico CHECK (quantidade > 0)
);

-- Itens de Peça (dentro de uma OS) — com Snapshot de Preço
CREATE TABLE itens_peca (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ordem_servico_id UUID NOT NULL REFERENCES ordens_servico(id) ON DELETE CASCADE,
    peca_id          UUID NOT NULL REFERENCES pecas(id),
    nome_peca        VARCHAR(255) NOT NULL,
    preco_snapshot   NUMERIC(10, 2) NOT NULL,
    quantidade       INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_qtd_peca CHECK (quantidade > 0)
);

-- Índices para consultas frequentes
CREATE INDEX idx_veiculos_cliente_id      ON veiculos(cliente_id);
CREATE INDEX idx_ordens_status            ON ordens_servico(status);
CREATE INDEX idx_ordens_veiculo_id        ON ordens_servico(veiculo_id);
CREATE INDEX idx_ordens_data_conclusao    ON ordens_servico(data_conclusao);
CREATE INDEX idx_itens_servico_ordem_id   ON itens_servico(ordem_servico_id);
CREATE INDEX idx_itens_peca_ordem_id      ON itens_peca(ordem_servico_id);
