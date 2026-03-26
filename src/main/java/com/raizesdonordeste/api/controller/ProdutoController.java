package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.produto.ProdutoUpdateDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoCreateDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoResponseDTO;
import com.raizesdonordeste.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
    @Operation(
            summary = "Criar produto",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar produto",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoUpdateDTO dto) {
        return ResponseEntity.ok(produtoService.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar produto por id",
            description = "Publico."
    )
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @GetMapping
    @Operation(
            summary = "Listar produtos",
            description = "Publico."
    )
    public ResponseEntity<Page<ProdutoResponseDTO>> listarTodos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarTodos(pageable));
    }

    @GetMapping("/ativos")
    @Operation(
            summary = "Listar produtos ativos",
            description = "Publico."
    )
    public ResponseEntity<Page<ProdutoResponseDTO>> listarAtivos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarAtivos(pageable));
    }

    @GetMapping("/buscar")
    @Operation(
            summary = "Buscar produtos por nome",
            description = "Publico."
    )
    public ResponseEntity<Page<ProdutoResponseDTO>> buscarPorNome(
            @RequestParam String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarPorNome(nome, pageable));
    }

    @GetMapping("/buscar-descricao")
    @Operation(
            summary = "Buscar produtos por descricao",
            description = "Publico."
    )
    public ResponseEntity<Page<ProdutoResponseDTO>> buscarPorDescricao(
            @RequestParam String descricao,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarPorDescricao(descricao, pageable));
    }

    @GetMapping("/faixa-preco")
    @Operation(
            summary = "Buscar produtos por faixa de preco",
            description = "Publico."
    )
    public ResponseEntity<Page<ProdutoResponseDTO>> buscarPorFaixaDePreco(
            @RequestParam BigDecimal precoMin,
            @RequestParam BigDecimal precoMax,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarPorFaixaDePreco(precoMin, precoMax, pageable));
    }

    @PatchMapping("/{id}/ativar")
    @Operation(
            summary = "Ativar produto",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        produtoService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desativar")
    @Operation(
            summary = "Desativar produto",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        produtoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deletar produto",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        produtoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}


