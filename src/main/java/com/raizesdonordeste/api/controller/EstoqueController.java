package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.estoque.EstoqueRespostaDTO;
import com.raizesdonordeste.api.dto.estoque.MovimentacaoEstoqueRespostaDTO;
import com.raizesdonordeste.api.dto.estoque.MovimentacaoEstoqueRequisicaoDTO;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.MovimentacaoEstoque;
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
    public ResponseEntity<Page<EstoqueRespostaDTO>> listarEstoques(
            @RequestParam(required = false) Long lojaId,
            @PageableDefault(size = 20, sort = "dataAtualizacao") Pageable pageable) {

        Page<EstoqueRespostaDTO> resposta = estoqueService.listarEstoquesPorLoja(lojaId, pageable)
                .map(this::toEstoqueRespostaDTO);

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/movimentacoes")
    public ResponseEntity<Page<MovimentacaoEstoqueRespostaDTO>> listarMovimentacoes(
            @RequestParam(required = false) Long lojaId,
            @RequestParam(required = false) Long produtoId,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {

        Page<MovimentacaoEstoqueRespostaDTO> resposta = estoqueService
                .listarMovimentacoesPorLoja(lojaId, produtoId, pageable)
                .map(this::toMovimentacaoRespostaDTO);

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/entrada")
    public ResponseEntity<EstoqueRespostaDTO> registrarEntrada(
            @RequestParam(required = false) Long lojaId,
            @Valid @RequestBody MovimentacaoEstoqueRequisicaoDTO dto) {

        Estoque estoque = estoqueService.registrarEntrada(lojaId, dto.getProdutoId(), dto.getQuantidade(), dto.getMotivo());

        return ResponseEntity.status(HttpStatus.CREATED).body(toEstoqueRespostaDTO(estoque));
    }

    @PostMapping("/saida")
    public ResponseEntity<EstoqueRespostaDTO> registrarSaida(
            @RequestParam(required = false) Long lojaId,
            @Valid @RequestBody MovimentacaoEstoqueRequisicaoDTO dto) {

        Estoque estoque = estoqueService.registrarSaida(lojaId, dto.getProdutoId(), dto.getQuantidade(), dto.getMotivo());

        return ResponseEntity.status(HttpStatus.OK).body(toEstoqueRespostaDTO(estoque));
    }

    private EstoqueRespostaDTO toEstoqueRespostaDTO(Estoque estoque) {
        return EstoqueRespostaDTO.builder()
                .id(estoque.getId())
                .lojaId(estoque.getLoja().getId())
                .lojaNome(estoque.getLoja().getNome())
                .produtoId(estoque.getProduto().getId())
                .produtoNome(estoque.getProduto().getNome())
                .quantidade(estoque.getQuantidade())
                .versao(estoque.getVersao())
                .dataCriacao(estoque.getDataCriacao())
                .dataAtualizacao(estoque.getDataAtualizacao())
                .build();
    }

    private MovimentacaoEstoqueRespostaDTO toMovimentacaoRespostaDTO(MovimentacaoEstoque movimentacao) {
        return MovimentacaoEstoqueRespostaDTO.builder()
                .id(movimentacao.getId())
                .estoqueId(movimentacao.getEstoque().getId())
                .lojaId(movimentacao.getEstoque().getLoja().getId())
                .produtoId(movimentacao.getEstoque().getProduto().getId())
                .tipo(movimentacao.getTipo())
                .quantidade(movimentacao.getQuantidade())
                .motivo(movimentacao.getMotivo())
                .usuarioId(movimentacao.getUsuario().getId())
                .usuarioNome(movimentacao.getUsuario().getNome())
                .dataCriacao(movimentacao.getDataCriacao())
                .build();
    }
}

