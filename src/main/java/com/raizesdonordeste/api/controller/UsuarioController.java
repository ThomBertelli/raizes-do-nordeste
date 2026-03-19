package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.usuario.UsuarioUpdateDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioCreateDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioResponseDTO;
import com.raizesdonordeste.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@Valid @RequestBody UsuarioCreateDTO dto) {
        UsuarioResponseDTO usuario = usuarioService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateDTO dto) {
        UsuarioResponseDTO usuario = usuarioService.atualizar(id, dto);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        UsuarioResponseDTO usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioResponseDTO>> listarTodos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<UsuarioResponseDTO> usuarios = usuarioService.listarTodos(pageable);
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/buscar")
    public ResponseEntity<Page<UsuarioResponseDTO>> buscarPorNome(
            @RequestParam String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<UsuarioResponseDTO> usuarios = usuarioService.buscarPorNome(nome, pageable);
        return ResponseEntity.ok(usuarios);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}

