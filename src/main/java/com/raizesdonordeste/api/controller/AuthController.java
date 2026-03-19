package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.auth.CadastroRequestDTO;
import com.raizesdonordeste.api.dto.auth.LoginRequestDTO;
import com.raizesdonordeste.api.dto.auth.LoginResponseDTO;
import com.raizesdonordeste.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        LoginResponseDTO response = authService.autenticar(loginRequestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cadastro")
    public ResponseEntity<LoginResponseDTO> cadastro(@Valid @RequestBody CadastroRequestDTO cadastroRequestDTO) {
        LoginResponseDTO response = authService.cadastrar(cadastroRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
