-- ============================================================
-- V7 — Marca temporal da última transição de status (Fase 3)
-- ============================================================

ALTER TABLE ordens_servico
    ADD COLUMN data_ultima_transicao TIMESTAMP;

UPDATE ordens_servico
    SET data_ultima_transicao = COALESCE(data_conclusao, data_inicio, data_abertura)
    WHERE data_ultima_transicao IS NULL;

ALTER TABLE ordens_servico
    ALTER COLUMN data_ultima_transicao SET NOT NULL;
