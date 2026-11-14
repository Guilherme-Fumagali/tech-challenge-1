CREATE TABLE historico_status_os (
    id             UUID PRIMARY KEY,
    os_id          UUID NOT NULL REFERENCES ordens_servico(id),
    status_origem  VARCHAR(30),
    status_destino VARCHAR(30) NOT NULL,
    evento         VARCHAR(60) NOT NULL,
    ocorrido_em    TIMESTAMP NOT NULL
);

CREATE INDEX idx_historico_status_os_os_id ON historico_status_os (os_id, ocorrido_em);
