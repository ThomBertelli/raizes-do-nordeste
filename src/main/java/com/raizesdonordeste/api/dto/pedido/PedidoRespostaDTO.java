package com.raizesdonordeste.api.dto.pedido;

import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoRespostaDTO {

    private Long id;
    private Long lojaId;
    private String lojaNome;
    private Long clienteId;
    private String clienteNome;
    private CanalPedido canalPedido;
    private StatusPedido statusPedido;
    private BigDecimal valorTotal;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}

