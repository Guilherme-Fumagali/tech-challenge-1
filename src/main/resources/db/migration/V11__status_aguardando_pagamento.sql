ALTER TABLE ordens_servico
    DROP CONSTRAINT chk_status;

ALTER TABLE ordens_servico
    ADD CONSTRAINT chk_status CHECK (status IN (
        'RECEBIDA', 'EM_DIAGNOSTICO', 'AGUARDANDO_APROVACAO', 'AGUARDANDO_PAGAMENTO',
        'EM_EXECUCAO', 'FINALIZADA', 'ENTREGUE', 'CANCELADA'
    ));

ALTER TABLE ordens_servico
    DROP COLUMN token_aprovacao_externa,
    DROP COLUMN token_expiracao;
