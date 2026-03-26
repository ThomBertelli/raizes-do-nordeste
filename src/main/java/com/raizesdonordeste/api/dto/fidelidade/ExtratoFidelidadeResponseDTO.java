package com.raizesdonordeste.api.dto.fidelidade;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Getter
@Builder
public class ExtratoFidelidadeResponseDTO {

    private BigDecimal saldo;
    private Page<ExtratoFidelidadeItemDTO> transacoes;
}

