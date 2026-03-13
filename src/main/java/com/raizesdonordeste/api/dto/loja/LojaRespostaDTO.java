package com.raizesdonordeste.api.dto.loja;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LojaRespostaDTO {

    private Long id;
    private String nome;
    private String cnpj;
    private String endereco;
    private boolean ativa;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}

