CREATE TABLE funcionarios (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cpf       VARCHAR(11) NOT NULL UNIQUE,
    nome      VARCHAR(255) NOT NULL,
    email     VARCHAR(255),
    status    VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_funcionario_status CHECK (status IN ('ATIVO', 'INATIVO')),
    CONSTRAINT chk_funcionario_cpf CHECK (cpf ~ '^[0-9]{11}$')
);
