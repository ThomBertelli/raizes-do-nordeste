package com.raizesdonordeste.api.dto.fidelidade;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SaldoFidelidadeResponseDTO {

    private BigDecimal saldo;
    private boolean consentimentoProgramaFidelidade;
}

