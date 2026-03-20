package com.raizesdonordeste.api.dto.pagamento;

import com.raizesdonordeste.domain.enums.StatusPedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoResponseDTO {

    private Long pedidoId;
    private String transacaoId;
    private boolean aprovado;
    private String statusPagamento;
    private StatusPedido statusPedido;
}

