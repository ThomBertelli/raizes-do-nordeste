package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.produto.ProdutoAtualizacaoDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoCriacaoDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoRespostaDTO;
import com.raizesdonordeste.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @PostMapping
    public ResponseEntity<ProdutoRespostaDTO> criar(@Valid @RequestBody ProdutoCriacaoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoRespostaDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoAtualizacaoDTO dto) {
        return ResponseEntity.ok(produtoService.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoRespostaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoRespostaDTO>> listarTodos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarTodos(pageable));
    }

    @GetMapping("/ativos")
    public ResponseEntity<Page<ProdutoRespostaDTO>> listarAtivos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarAtivos(pageable));
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<ProdutoRespostaDTO>> buscarPorNome(
            @RequestParam String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarPorNome(nome, pageable));
    }

    @GetMapping("/buscar-descricao")
    public ResponseEntity<Page<ProdutoRespostaDTO>> buscarPorDescricao(
            @RequestParam String descricao,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarPorDescricao(descricao, pageable));
    }

    @GetMapping("/faixa-preco")
    public ResponseEntity<Page<ProdutoRespostaDTO>> buscarPorFaixaDePreco(
            @RequestParam BigDecimal precoMin,
            @RequestParam BigDecimal precoMax,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarPorFaixaDePreco(precoMin, precoMax, pageable));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        produtoService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        produtoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}


