package com.raizesdonordeste.api.controller;

import com.raizesdonordeste.api.dto.auth.LoginRequest;
import com.raizesdonordeste.api.dto.auth.LoginResponse;
import com.raizesdonordeste.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.autenticar(loginRequest);
        return ResponseEntity.ok(response);
    }
}

