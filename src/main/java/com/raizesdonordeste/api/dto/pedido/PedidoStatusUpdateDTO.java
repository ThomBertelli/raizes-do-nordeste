package com.raizesdonordeste.api.dto.pedido;

import com.raizesdonordeste.domain.enums.StatusPedido;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoStatusUpdateDTO {

    @NotNull(message = "statusPedido é obrigatório")
    private StatusPedido statusPedido;

    @NotNull(message = "origem é obrigatória")
    private OrigemStatusPedido origem;

    public enum OrigemStatusPedido {
        PAGAMENTO,
        OPERACAO_LOJA
    }
}

