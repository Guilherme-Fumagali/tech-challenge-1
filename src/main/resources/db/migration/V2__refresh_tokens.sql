-- V2 — Tabela de Refresh Tokens
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token       VARCHAR(36) NOT NULL UNIQUE,
    username    VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    criado_em   TIMESTAMP NOT NULL DEFAULT NOW(),
    revogado    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_tokens_token    ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_username ON refresh_tokens(username);
