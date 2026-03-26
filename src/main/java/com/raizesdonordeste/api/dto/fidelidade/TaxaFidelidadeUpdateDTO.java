package com.raizesdonordeste.api.dto.fidelidade;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TaxaFidelidadeUpdateDTO {

    @NotNull(message = "taxaConversao é obrigatória")
    private BigDecimal taxaConversao;
}

