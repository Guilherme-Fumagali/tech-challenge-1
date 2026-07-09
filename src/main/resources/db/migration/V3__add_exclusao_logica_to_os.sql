ALTER TABLE ordens_servico
    ADD COLUMN excluida_logicamente BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN data_exclusao_logica TIMESTAMP;
