package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.loja.LojaUpdateDTO;
import com.raizesdonordeste.api.dto.loja.LojaCreateDTO;
import com.raizesdonordeste.api.dto.loja.LojaResponseDTO;
import com.raizesdonordeste.service.LojaService;
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

    @PostMapping
    public ResponseEntity<LojaResponseDTO> criar(@Valid @RequestBody LojaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lojaService.criar(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LojaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody LojaUpdateDTO dto) {
        return ResponseEntity.ok(lojaService.atualizar(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LojaResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(lojaService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<Page<LojaResponseDTO>> listarTodos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(lojaService.listarTodos(pageable));
    }

    @GetMapping("/ativas")
    public ResponseEntity<Page<LojaResponseDTO>> listarAtivas(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(lojaService.buscarAtivas(pageable));
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<LojaResponseDTO>> buscarPorNome(
            @RequestParam String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(lojaService.buscarPorNome(nome, pageable));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        lojaService.ativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        lojaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        lojaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}

