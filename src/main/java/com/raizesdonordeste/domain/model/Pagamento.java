package com.raizesdonordeste.domain.model;

import com.raizesdonordeste.domain.enums.FormaPagamento;
import com.raizesdonordeste.domain.enums.StatusPagamento;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    @NotNull(message = "Pedido é obrigatório")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Forma de pagamento é obrigatória")
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status do pagamento é obrigatório")
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal valor;

    @Column(length = 100)
    @Size(max = 100, message = "Código da transação deve ter no máximo 100 caracteres")
    private String codigoTransacao;

    @Column(length = 500)
    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    private String observacao;

    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;
}

