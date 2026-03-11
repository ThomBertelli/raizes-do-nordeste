package com.raizesdonordeste.domain.enums;

public enum PerfilUsuario {
    ADMIN("Administrador"),
    CLIENTE("Cliente"),
    FUNCIONARIO("Funcionário"),
    GERENTE("Gerente"),
    GERENCIA_MATRIZ("Gerência da Matriz");

    private final String descricao;

    PerfilUsuario(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
