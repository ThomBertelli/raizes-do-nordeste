package com.raizesdonordeste.domain.enums;

public enum StatusPedido {

    CRIADO(1, "Criado"),
    CONFIRMADO(2, "Confirmado"),
    PREPARO(3, "Em Preparo"),
    PRONTO(4, "Pronto para Entrega"),
    ENTREGUE(5, "Entregue"),
    CANCELADO(6, "Cancelado");

    private int cod;
    private String descricao;

    private StatusPedido(int cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }

}
