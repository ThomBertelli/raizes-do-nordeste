package com.raizesdonordeste.api.dto.estoque;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstoqueRequestDTO {

    private Long id;
    private Long lojaId;
    private String lojaNome;
    private Long produtoId;
    private String produtoNome;
    private Integer quantidade;
    private Long versao;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}

