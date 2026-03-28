package com.raizesdonordeste.api.dto.produto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProdutoDisponivelLojaResponseDTO {

    private String nome;
    private String descricao;
    private BigDecimal preco;
}
