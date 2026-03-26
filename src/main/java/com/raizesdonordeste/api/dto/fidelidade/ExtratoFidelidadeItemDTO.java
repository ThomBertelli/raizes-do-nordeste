package com.raizesdonordeste.api.dto.fidelidade;

import com.raizesdonordeste.domain.enums.TipoTransacaoFidelidade;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ExtratoFidelidadeItemDTO {

    private Long id;
    private Long pedidoId;
    private TipoTransacaoFidelidade tipo;
    private BigDecimal moedas;
    private LocalDateTime dataCriacao;
}

