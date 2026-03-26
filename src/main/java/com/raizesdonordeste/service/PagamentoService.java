package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pagamento.PagamentoRequestDTO;
import com.raizesdonordeste.api.dto.pagamento.PagamentoResponseDTO;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.repository.PedidoRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.infra.request.IdempotentResponse;
import com.raizesdonordeste.infra.request.RequestDeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {

    private final PedidoRepository pedidoRepository;
    private final MockPaymentGateway mockPaymentGateway;
    private final RequestDeduplicationService requestDeduplicationService;
    private final SecurityContextService securityContextService;
    private final JsonMapper objectMapper;

    @Transactional
    public PagamentoResponseDTO processarPagamento(Long pedidoId, PagamentoRequestDTO request) {
        return processarPagamentoInterno(pedidoId, request, null);
    }

    @Transactional
    public IdempotentResponse<PagamentoResponseDTO> processarPagamentoComIdempotencia(
            Long pedidoId,
            PagamentoRequestDTO request,
            String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            PagamentoResponseDTO response = processarPagamento(pedidoId, request);
            return new IdempotentResponse<>(response, HttpStatus.OK.value());
        }

        String requestHash = calcularHashPagamento(pedidoId, request);

        return requestDeduplicationService.execute(
                idempotencyKey,
                resolverUsuarioId(),
                requestHash,
                PagamentoResponseDTO.class,
                () -> processarPagamentoInterno(pedidoId, request, idempotencyKey),
                HttpStatus.OK.value()
        );
    }

    private PagamentoResponseDTO processarPagamentoInterno(Long pedidoId,
                                                           PagamentoRequestDTO request,
                                                           String idempotencyKey) {
        Pedido pedido = pedidoRepository.findByIdWithRelacionamentos(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + pedidoId));

        if (pedido.getStatusPedido() != StatusPedido.CRIADO) {
            throw new IllegalStateException("Pagamento so pode ser processado para pedidos CRIADO");
        }

        log.info("Pagamento recebido: pedidoId={}, valor={}", pedidoId, request.getValor());

        MockPaymentGateway.MockPaymentResult resultado = mockPaymentGateway.processarPagamento(request.getValor(), idempotencyKey);

        StatusPedido novoStatus = resultado.aprovado() ? StatusPedido.CONFIRMADO : StatusPedido.CANCELADO;
        pedido.setStatusPedido(novoStatus);
        pedidoRepository.save(pedido);

        log.info("Pagamento processado: pedidoId={}, aprovado={}, statusPagamento={}, statusPedido={}",
                pedidoId,
                resultado.aprovado(),
                resultado.status(),
                novoStatus);

        return PagamentoResponseDTO.builder()
                .pedidoId(pedidoId)
                .transacaoId(resultado.transacaoId())
                .aprovado(resultado.aprovado())
                .statusPagamento(resultado.status())
                .statusPedido(novoStatus)
                .build();
    }

    private String calcularHashPagamento(Long pedidoId, PagamentoRequestDTO request) {
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(request);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar pagamento para hash", ex);
        }
        String payload = pedidoId + "|" + requestJson;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", ex);
        }
    }

    private Long resolverUsuarioId() {
        return securityContextService.getRequiredPrincipal().getId();
    }
}
