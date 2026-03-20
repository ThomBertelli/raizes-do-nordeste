package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pagamento.PagamentoRequestDTO;
import com.raizesdonordeste.api.dto.pagamento.PagamentoResponseDTO;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.repository.PedidoRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {

    private final PedidoRepository pedidoRepository;
    private final MockPaymentGateway mockPaymentGateway;

    @Transactional
    public PagamentoResponseDTO processarPagamento(Long pedidoId, PagamentoRequestDTO request) {
        Pedido pedido = pedidoRepository.findByIdWithRelacionamentos(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + pedidoId));

        if (pedido.getStatusPedido() != StatusPedido.CRIADO) {
            throw new IllegalStateException("Pagamento so pode ser processado para pedidos CRIADO");
        }

        log.info("Pagamento recebido: pedidoId={}, valor={}", pedidoId, request.getValor());

        MockPaymentGateway.MockPaymentResult resultado = mockPaymentGateway.processarPagamento(request.getValor());

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
}

