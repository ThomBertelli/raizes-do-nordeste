package com.raizesdonordeste.service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway {

    private final Map<String, MockPaymentResult> idempotentResults = new ConcurrentHashMap<>();

    public MockPaymentResult processarPagamento(BigDecimal valor) {
        return processarPagamento(valor, null);
    }

    public MockPaymentResult processarPagamento(BigDecimal valor, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotentResults.computeIfAbsent(idempotencyKey, key -> executarPagamento(valor));
        }
        return executarPagamento(valor);
    }

    private MockPaymentResult executarPagamento(BigDecimal valor) {
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
