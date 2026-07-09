ALTER TABLE ordens_servico
    ADD COLUMN token_aprovacao_externa VARCHAR(64),
    ADD COLUMN token_expiracao TIMESTAMP;
