package com.raizesdonordeste.domain.enums;

public enum TipoMovimentacaoEstoque {
    ENTRADA("Entrada"),
    AJUSTE("Ajuste"),
    SAIDA("Saída");

    private final String descricao;

    TipoMovimentacaoEstoque(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
