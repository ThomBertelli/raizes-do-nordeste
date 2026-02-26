package com.raizesdonordeste.domain.enums;


import lombok.Getter;
import lombok.Setter;

public enum PerfilUsuario {
    ADMIN(1, "ROLE_ADMIN"),
    CLIENTE(2, "ROLE_CLIENTE"),
    FUNCIONARIO(3, "ROLE_FUNCIONARIO"),
    GERENTE(4, "ROLE_GERENTE"),
    GERENCIA_MATRIZ(5, "ROLE_GERENCIA_MATRIZ");



    private int cod;
    private String descricao;

    private PerfilUsuario(int cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }


}
