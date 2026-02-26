package com.raizesdonordeste.api.domain.enums;

public enum StatusPagamento {

    PENDENTE(1, "Pendente"),
    APROVADO(2, "Aprovado"),
    RECUSADO(3, "Recusado"),
    CANCELADO(4, "Cancelado");

    private int cod;
    private final String descricao;

    private StatusPagamento(int cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }
}
