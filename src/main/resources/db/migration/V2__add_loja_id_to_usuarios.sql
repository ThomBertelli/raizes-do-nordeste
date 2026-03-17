ALTER TABLE usuarios
    ADD COLUMN loja_id BIGINT;

ALTER TABLE usuarios
    ADD CONSTRAINT fk_usuarios_loja
        FOREIGN KEY (loja_id) REFERENCES lojas (id);

CREATE INDEX idx_usuarios_loja_id ON usuarios (loja_id);

