package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.auth.CadastroRequest;
import com.raizesdonordeste.api.dto.auth.LoginRequest;
import com.raizesdonordeste.api.dto.auth.LoginResponse;
import com.raizesdonordeste.config.JwtTokenProvider;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public LoginResponse cadastrar(CadastroRequest cadastroRequest) {

        if (usuarioRepository.existsByEmail(cadastroRequest.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(cadastroRequest.getNome())
                .email(cadastroRequest.getEmail())
                .senha(passwordEncoder.encode(cadastroRequest.getSenha()))
                .perfil(PerfilUsuario.CLIENTE)
                .ativo(true)
                .consentimentoProgramaFidelidade(cadastroRequest.isConsentimentoProgramaFidelidade())
                .build();

        Usuario salvo = usuarioRepository.save(usuario);

        String token = jwtTokenProvider.gerarToken(salvo.getEmail());

        return LoginResponse.builder()
                .accessToken(token)
                .expiresIn(jwtExpiration)
                .id(salvo.getId())
                .nome(salvo.getNome())
                .email(salvo.getEmail())
                .perfil(salvo.getPerfil())
                .build();
    }
}
