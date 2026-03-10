package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.auth.LoginRequest;
import com.raizesdonordeste.api.dto.auth.LoginResponse;
import com.raizesdonordeste.config.JwtTokenProvider;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getSenha()
                    )
            );

            String token = jwtTokenProvider.gerarToken(authentication);

            Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

            return LoginResponse.builder()
                    .accessToken(token)
                    .expiresIn(jwtExpiration)
                    .id(usuario.getId())
                    .nome(usuario.getNome())
                    .email(usuario.getEmail())
                    .perfil(usuario.getPerfil())
                    .build();

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Email ou senha inválidos");
        }
    }
}

