package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.pagamento.PagamentoRequestDTO;
import com.raizesdonordeste.api.dto.pagamento.PagamentoResponseDTO;
import com.raizesdonordeste.infra.request.IdempotentResponse;
import com.raizesdonordeste.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping("/{pedidoId}")
    @Operation(
            summary = "Processar pagamento",
            description = "Requer autenticacao. Perfil: CLIENTE."
    )
    public ResponseEntity<PagamentoResponseDTO> processarPagamento(
            @PathVariable Long pedidoId,
            @Valid @RequestBody PagamentoRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        IdempotentResponse<PagamentoResponseDTO> resposta = pagamentoService
                .processarPagamentoComIdempotencia(pedidoId, request, idempotencyKey);
        return ResponseEntity.status(resposta.statusCode()).body(resposta.body());
    }
}
