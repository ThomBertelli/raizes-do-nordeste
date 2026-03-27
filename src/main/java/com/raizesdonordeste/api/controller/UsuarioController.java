package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.usuario.UsuarioUpdateDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioCreateResponseDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioCreateDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioResponseDTO;
import com.raizesdonordeste.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @Operation(
            summary = "Criar usuario",
            description = "Requer autenticacao. Perfis: ADMIN, GERENTE, GERENCIA_MATRIZ."
    )
    public ResponseEntity<UsuarioCreateResponseDTO> criar(@Valid @RequestBody UsuarioCreateDTO dto) {
        UsuarioResponseDTO usuario = usuarioService.criar(dto);
        UsuarioCreateResponseDTO response = UsuarioCreateResponseDTO.builder()
                .usuario(usuario.getNome())
                .mensagem("criado com sucesso")
                .perfil(usuario.getPerfil())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar usuario",
            description = "Requer autenticacao. Perfis: ADMIN, GERENCIA_MATRIZ."
    )
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateDTO dto) {
        UsuarioResponseDTO usuario = usuarioService.atualizar(id, dto);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar usuario por id",
            description = "Requer autenticacao. Perfis: ADMIN, GERENCIA_MATRIZ."
    )
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable Long id) {
        UsuarioResponseDTO usuario = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(usuario);
    }

    @GetMapping
    @Operation(
            summary = "Listar usuarios",
            description = "Requer autenticacao. Perfis: ADMIN, GERENCIA_MATRIZ."
    )
    public ResponseEntity<Page<UsuarioResponseDTO>> listarTodos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<UsuarioResponseDTO> usuarios = usuarioService.listarTodos(pageable);
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/buscar")
    @Operation(
            summary = "Buscar usuarios por nome",
            description = "Requer autenticacao. Perfis: ADMIN, GERENCIA_MATRIZ."
    )
    public ResponseEntity<Page<UsuarioResponseDTO>> buscarPorNome(
            @RequestParam String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<UsuarioResponseDTO> usuarios = usuarioService.buscarPorNome(nome, pageable);
        return ResponseEntity.ok(usuarios);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Deletar usuario",
            description = "Requer autenticacao. Perfis: ADMIN, GERENCIA_MATRIZ."
    )
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        usuarioService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desativar")
    @Operation(
            summary = "Desativar usuario",
            description = "Requer autenticacao. Perfis: ADMIN, GERENCIA_MATRIZ."
    )
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    @Operation(
            summary = "Ativar usuario",
            description = "Requer autenticacao. Perfis: ADMIN, GERENCIA_MATRIZ."
    )
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        usuarioService.ativar(id);
        return ResponseEntity.noContent().build();
    }
}
