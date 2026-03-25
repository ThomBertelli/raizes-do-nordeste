package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.pedido.PedidoRequestDTO;
import com.raizesdonordeste.api.dto.pedido.PedidoResponseDTO;
import com.raizesdonordeste.api.dto.pedido.PedidoStatusUpdateDTO;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.infra.request.IdempotentResponse;
import com.raizesdonordeste.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criar(
            @Valid @RequestBody PedidoRequestDTO dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        IdempotentResponse<PedidoResponseDTO> resposta = pedidoService.criarComIdempotencia(dto, idempotencyKey);
        return ResponseEntity.status(resposta.statusCode()).body(resposta.body());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<PedidoResponseDTO>> listarPorLoja(
            @RequestParam(required = false) Long lojaId,
            @RequestParam(required = false) CanalPedido canalPedido,
            @RequestParam(required = false) StatusPedido statusPedido,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarPorLoja(lojaId, canalPedido, statusPedido, pageable));
    }

    @GetMapping("/meus")
    public ResponseEntity<Page<PedidoResponseDTO>> listarMeusPedidos(
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        return ResponseEntity.ok(pedidoService.listarMeusPedidos(pageable));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody PedidoStatusUpdateDTO dto) {
        return ResponseEntity.ok(pedidoService.atualizarStatusOperacaoLoja(id, dto));
    }
}
