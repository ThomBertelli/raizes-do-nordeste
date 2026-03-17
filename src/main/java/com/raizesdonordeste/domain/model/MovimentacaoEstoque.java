package com.raizesdonordeste.domain.model;

import com.raizesdonordeste.domain.enums.TipoMovimentacaoEstoque;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estoque_id", nullable = false)
    @NotNull(message = "Estoque é obrigatório")
    private Estoque estoque;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Tipo de movimentação é obrigatório")
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false)
    @NotNull(message = "Quantidade é obrigatória")
    @Min(value = 1, message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    @Column(length = 500)
    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "Usuário é obrigatório")
    private Usuario usuario;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

}

