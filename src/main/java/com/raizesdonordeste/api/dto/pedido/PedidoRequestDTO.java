package com.raizesdonordeste.api.dto.pedido;

import com.raizesdonordeste.domain.enums.CanalPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequestDTO {

    @NotNull(message = "lojaId é obrigatório")
    private Long lojaId;

    @NotNull(message = "canalPedido é obrigatório")
    private CanalPedido canalPedido;

    @NotEmpty(message = "itens são obrigatórios")
    @Valid
    private List<PedidoItemRequestDTO> itens;

    private java.math.BigDecimal moedasFidelidade;
}
