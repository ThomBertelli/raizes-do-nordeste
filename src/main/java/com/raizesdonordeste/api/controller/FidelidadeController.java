package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.fidelidade.ExtratoFidelidadeResponseDTO;
import com.raizesdonordeste.api.dto.fidelidade.SaldoFidelidadeResponseDTO;
import com.raizesdonordeste.api.dto.fidelidade.TaxaFidelidadeResponseDTO;
import com.raizesdonordeste.api.dto.fidelidade.TaxaFidelidadeUpdateDTO;
import com.raizesdonordeste.service.ConfiguracaoFidelidadeService;
import com.raizesdonordeste.service.FidelidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/fidelidade")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FidelidadeController {

    private final FidelidadeService fidelidadeService;
    private final ConfiguracaoFidelidadeService configuracaoFidelidadeService;

    @GetMapping("/saldo")
    @Operation(
            summary = "Consultar saldo de fidelidade",
            description = "Requer autenticacao. Perfil: CLIENTE."
    )
    public ResponseEntity<SaldoFidelidadeResponseDTO> consultarSaldo() {
        return ResponseEntity.ok(fidelidadeService.consultarSaldoCliente());
    }

    @GetMapping("/extrato")
    @Operation(
            summary = "Consultar extrato de fidelidade",
            description = "Requer autenticacao. Perfil: CLIENTE."
    )
    public ResponseEntity<ExtratoFidelidadeResponseDTO> consultarExtrato(
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        return ResponseEntity.ok(fidelidadeService.consultarExtratoCliente(pageable));
    }

    @GetMapping("/taxa")
    @Operation(
            summary = "Consultar taxa de conversao",
            description = "Requer autenticacao. Perfil: GERENCIA_MATRIZ."
    )
    public ResponseEntity<TaxaFidelidadeResponseDTO> consultarTaxaConversao() {
        return ResponseEntity.ok(configuracaoFidelidadeService.consultarTaxaConversao());
    }

    @PatchMapping("/taxa")
    @Operation(
            summary = "Atualizar taxa de conversao",
            description = "Requer autenticacao. Perfil: GERENCIA_MATRIZ."
    )
    public ResponseEntity<TaxaFidelidadeResponseDTO> atualizarTaxaConversao(
            @Valid @RequestBody TaxaFidelidadeUpdateDTO request) {
        return ResponseEntity.ok(
                configuracaoFidelidadeService.atualizarTaxaConversao(request.getTaxaConversao())
        );
    }
}

