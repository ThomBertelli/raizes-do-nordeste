package com.raizesdonordeste.domain.enums;

public enum StatusPedido {
    CRIADO("Criado"),
    CONFIRMADO("Confirmado"),
    PREPARO("Em Preparo"),
    PRONTO("Pronto para Entrega"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
