package com.raizesdonordeste.domain.enums;

public enum CanalPedido {

    APP(1, "Aplicativo"),
    SITE(2, "Site"),
    TOTEM(3, "Totem de Autoatendimento"),
    PICKUP(4, "Retirada no Local"),
    BALCAO(5, "Pedido no Balcão da Loja");

    private final int cod;
    private final String descricao;

    private CanalPedido(int cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }
}
