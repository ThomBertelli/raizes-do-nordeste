package com.raizesdonordeste.api.domain.enums;

public enum FormaPagamento {

    DINHEIRO(1, "Dinheiro"),
    CARTAO_CREDITO(2, "Cartão de Crédito"),
    CARTAO_DEBITO(3, "Cartão de Débito"),
    PIX(4, "Pix"),
    VALE_ALIMENTACAO(5, "Vale Alimentação"),
    VALE_REFEICAO(6, "Vale Refeição");

    private int cod;
    private String descricao;

    private FormaPagamento(int cod, String descricao) {
        this.cod = cod;
        this.descricao = descricao;
    }
}
