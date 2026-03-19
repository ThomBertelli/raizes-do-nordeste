package com.raizesdonordeste.api.dto.loja;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LojaUpdateDTO {

    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    private String cnpj;

    @Size(min = 5, max = 255, message = "Endereço deve ter entre 5 e 255 caracteres")
    private String endereco;

    private Boolean ativa;
}

