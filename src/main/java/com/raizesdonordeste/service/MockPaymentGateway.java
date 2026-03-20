package com.raizesdonordeste.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway {

    public MockPaymentResult processarPagamento(BigDecimal valor) {
        boolean aprovado = valor != null
                && valor.compareTo(BigDecimal.ZERO) > 0
                && valor.compareTo(new BigDecimal("1000.00")) <= 0;

        return new MockPaymentResult(
                UUID.randomUUID().toString(),
                aprovado,
                aprovado ? "APROVADO" : "RECUSADO"
        );
    }

    public record MockPaymentResult(String transacaoId, boolean aprovado, String status) {
    }
}

