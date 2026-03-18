DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_estoques_loja_produto'
    ) THEN
        ALTER TABLE estoques
            ADD CONSTRAINT uk_estoques_loja_produto UNIQUE (loja_id, produto_id);
    END IF;
END $$;

