ALTER TABLE saldos_fidelidade
ADD CONSTRAINT ck_saldos_fidelidade_moedas_nao_negativo CHECK (moedas >= 0);

