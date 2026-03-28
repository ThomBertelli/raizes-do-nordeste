package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.loja.LojaUpdateDTO;
import com.raizesdonordeste.api.dto.loja.LojaCreateDTO;
import com.raizesdonordeste.api.dto.loja.LojaResponseDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoDisponivelLojaResponseDTO;
import com.raizesdonordeste.service.LojaService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lojas")
@RequiredArgsConstructor
public class LojaController {

    private final LojaService lojaService;
    private final ProdutoService produtoService;

    @PostMapping
    @Operation(
            summary = "Criar loja",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<LojaResponseDTO> criar(@Valid @RequestBody LojaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lojaService.criar(dto));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar loja",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<LojaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody LojaUpdateDTO dto) {
        return ResponseEntity.ok(lojaService.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar loja por id",
            description = "Publico."
    )
    public ResponseEntity<LojaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(lojaService.buscarPorId(id));
    }

    @GetMapping
    @Operation(
            summary = "Listar lojas",
            description = "Publico."
    )
    public ResponseEntity<Page<LojaResponseDTO>> listarTodos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(lojaService.listarTodos(pageable));
    }

    @GetMapping("/ativas")
    @Operation(
            summary = "Listar lojas ativas",
            description = "Publico."
    )
    public ResponseEntity<Page<LojaResponseDTO>> listarAtivas(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(lojaService.buscarAtivas(pageable));
    }

    @GetMapping("/buscar")
    @Operation(
            summary = "Buscar lojas por nome",
            description = "Publico."
    )
    public ResponseEntity<Page<LojaResponseDTO>> buscarPorNome(
            @RequestParam String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(lojaService.buscarPorNome(nome, pageable));
    }

    @GetMapping("/{id}/produtos-disponiveis")
    @Operation(
            summary = "Listar produtos disponíveis para compra em uma loja",
            description = "Publico. Retorna apenas produtos ativos com estoque maior que zero na loja informada."
    )
    public ResponseEntity<Page<ProdutoDisponivelLojaResponseDTO>> listarProdutosDisponiveis(
            @PathVariable Long id,
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 20, sort = "produto.nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarDisponiveisPorLoja(id, nome, pageable));
    }

    @PatchMapping("/{id}/ativar")
    @Operation(
            summary = "Ativar loja",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        lojaService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desativar")
    @Operation(
            summary = "Desativar loja",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        lojaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deletar loja",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        lojaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

