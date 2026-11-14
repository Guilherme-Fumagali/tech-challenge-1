CREATE TABLE sagas_ordem_servico (
    id                     UUID PRIMARY KEY,
    os_id                  UUID NOT NULL UNIQUE REFERENCES ordens_servico(id),
    estado                 VARCHAR(40) NOT NULL,
    motivo_compensacao     VARCHAR(40),
    compensacoes_pendentes VARCHAR(200) NOT NULL DEFAULT '',
    ultimo_comando_tipo    VARCHAR(60),
    ultimo_comando_dados   JSONB,
    prazo_resposta         TIMESTAMP,
    tentativas_reenvio     INT NOT NULL DEFAULT 0,
    criada_em              TIMESTAMP NOT NULL,
    atualizada_em          TIMESTAMP NOT NULL,
    versao                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sagas_prazo_resposta ON sagas_ordem_servico (prazo_resposta)
    WHERE prazo_resposta IS NOT NULL;

CREATE TABLE saga_passos (
    id            UUID PRIMARY KEY,
    saga_id       UUID NOT NULL REFERENCES sagas_ordem_servico(id),
    sequencia     INT  NOT NULL,
    direcao       VARCHAR(10) NOT NULL,
    tipo          VARCHAR(60) NOT NULL,
    mensagem_id   UUID,
    resultado     VARCHAR(20) NOT NULL,
    detalhe       VARCHAR(500),
    ocorrido_em   TIMESTAMP NOT NULL,
    UNIQUE (saga_id, sequencia),
    CONSTRAINT chk_saga_passo_direcao CHECK (direcao IN ('ENVIADO', 'RECEBIDO', 'LOCAL')),
    CONSTRAINT chk_saga_passo_resultado CHECK (resultado IN ('APLICADO', 'IGNORADO', 'REJEITADO'))
);
