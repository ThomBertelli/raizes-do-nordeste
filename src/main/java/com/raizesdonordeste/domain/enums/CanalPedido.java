package com.raizesdonordeste.domain.enums;

public enum CanalPedido {
    APP("Aplicativo"),
    SITE("Site"),
    TOTEM("Totem de Autoatendimento"),
    PICKUP("Retirada no Local"),
    BALCAO("Pedido no Balcão da Loja");

    private final String descricao;

    CanalPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
