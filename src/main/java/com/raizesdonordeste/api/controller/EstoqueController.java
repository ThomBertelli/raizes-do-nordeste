package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.estoque.EstoqueRequestDTO;
import com.raizesdonordeste.api.dto.estoque.MovimentacaoEstoqueResponseDTO;
import com.raizesdonordeste.api.dto.estoque.MovimentacaoEstoqueRequestDTO;
import com.raizesdonordeste.service.EstoqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estoques")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping
    public ResponseEntity<Page<EstoqueRequestDTO>> listarEstoques(
            @RequestParam(required = false) Long lojaId,
            @PageableDefault(size = 20, sort = "dataAtualizacao") Pageable pageable) {

        Page<EstoqueRequestDTO> resposta = estoqueService.listarEstoquesPorLojaDTO(lojaId, pageable);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/movimentacoes")
    public ResponseEntity<Page<MovimentacaoEstoqueResponseDTO>> listarMovimentacoes(
            @RequestParam(required = false) Long lojaId,
            @RequestParam(required = false) Long produtoId,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {

        Page<MovimentacaoEstoqueResponseDTO> resposta = estoqueService
                .listarMovimentacoesPorLojaDTO(lojaId, produtoId, pageable);

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/entrada")
    public ResponseEntity<EstoqueRequestDTO> registrarEntrada(
            @RequestParam(required = false) Long lojaId,
            @Valid @RequestBody MovimentacaoEstoqueRequestDTO dto) {

        EstoqueRequestDTO resposta = estoqueService
                .registrarEntradaDTO(lojaId, dto.getProdutoId(), dto.getQuantidade(), dto.getMotivo());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/saida")
    public ResponseEntity<EstoqueRequestDTO> registrarSaida(
            @RequestParam(required = false) Long lojaId,
            @Valid @RequestBody MovimentacaoEstoqueRequestDTO dto) {

        EstoqueRequestDTO resposta = estoqueService
                .registrarSaidaDTO(lojaId, dto.getProdutoId(), dto.getQuantidade(), dto.getMotivo());

        return ResponseEntity.status(HttpStatus.OK).body(resposta);
    }
}
