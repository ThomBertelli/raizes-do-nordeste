ALTER TABLE configuracoes_fidelidade
ADD COLUMN singleton BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE configuracoes_fidelidade
SET singleton = TRUE;

ALTER TABLE configuracoes_fidelidade
ADD CONSTRAINT uq_configuracoes_fidelidade_singleton UNIQUE (singleton);

