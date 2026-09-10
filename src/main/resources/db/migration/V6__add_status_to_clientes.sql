-- ============================================================
-- V6 — Status do cliente + índices de suporte (Fase 3)
-- ============================================================

ALTER TABLE clientes
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';

ALTER TABLE clientes
    ADD CONSTRAINT chk_cliente_status
    CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADO'));

CREATE INDEX idx_clientes_cpf_status ON clientes (cpf_cnpj, status);

CREATE INDEX idx_os_status_abertura ON ordens_servico (status, data_abertura);
