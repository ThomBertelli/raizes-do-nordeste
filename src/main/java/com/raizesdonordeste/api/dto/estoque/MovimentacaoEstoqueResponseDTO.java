package com.raizesdonordeste.api.dto.estoque;

import com.raizesdonordeste.domain.enums.TipoMovimentacaoEstoque;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoqueResponseDTO {

    private Long id;
    private Long estoqueId;
    private Long lojaId;
    private Long produtoId;
    private TipoMovimentacaoEstoque tipo;
    private Integer quantidade;
    private String motivo;
    private Long usuarioId;
    private String usuarioNome;
    private LocalDateTime dataCriacao;
}

