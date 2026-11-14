CREATE TABLE outbox (
    id              UUID PRIMARY KEY,
    fila            VARCHAR(120) NOT NULL,
    tipo            VARCHAR(60)  NOT NULL,
    versao          INT          NOT NULL,
    os_id           UUID         NOT NULL,
    payload         JSONB        NOT NULL,
    cabecalhos      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    criada_em       TIMESTAMP    NOT NULL,
    publicada_em    TIMESTAMP,
    tentativas      INT          NOT NULL DEFAULT 0,
    ultimo_erro     VARCHAR(500)
);

CREATE INDEX idx_outbox_pendentes ON outbox (criada_em) WHERE publicada_em IS NULL;

CREATE TABLE mensagens_processadas (
    mensagem_id   UUID PRIMARY KEY,
    tipo          VARCHAR(60) NOT NULL,
    processada_em TIMESTAMP   NOT NULL
);
