package com.raizesdonordeste.api.dto.fidelidade;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TaxaFidelidadeResponseDTO {

    private BigDecimal taxaConversao;
}

