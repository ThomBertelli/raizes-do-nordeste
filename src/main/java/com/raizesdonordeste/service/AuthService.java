package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.auth.CadastroRequestDTO;
import com.raizesdonordeste.api.dto.auth.LoginRequestDTO;
import com.raizesdonordeste.api.dto.auth.LoginResponseDTO;
import com.raizesdonordeste.config.JwtTokenProvider;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import com.raizesdonordeste.exception.EmailJaCadastradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Transactional(readOnly = true)
    public LoginResponseDTO autenticar(LoginRequestDTO loginRequestDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDTO.getEmail(),
                            loginRequestDTO.getSenha()
                    )
            );

            String token = jwtTokenProvider.gerarToken(authentication);

            Usuario usuario = usuarioRepository.findByEmail(loginRequestDTO.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

            log.info("Login realizado com sucesso: usuarioId={}, perfil={}", usuario.getId(), usuario.getPerfil());

            return LoginResponseDTO.builder()
                    .accessToken(token)
                    .expiresIn(jwtExpiration)
                    .id(usuario.getId())
                    .nome(usuario.getNome())
                    .email(usuario.getEmail())
                    .perfil(usuario.getPerfil())
                    .lojaId(usuario.getLoja() != null ? usuario.getLoja().getId() : null)
                    .build();

        } catch (AuthenticationException e) {
            log.warn("Falha de autenticacao de usuario");
            throw new BadCredentialsException("Email ou senha inválidos");
        }
    }

    @Transactional
    public LoginResponseDTO cadastrar(CadastroRequestDTO cadastroRequestDTO) {

        if (usuarioRepository.existsByEmail(cadastroRequestDTO.getEmail())) {
            throw new EmailJaCadastradoException("Email já cadastrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(cadastroRequestDTO.getNome())
                .email(cadastroRequestDTO.getEmail())
                .senha(passwordEncoder.encode(cadastroRequestDTO.getSenha()))
                .perfil(PerfilUsuario.CLIENTE)
                .ativo(true)
                .consentimentoProgramaFidelidade(cadastroRequestDTO.isConsentimentoProgramaFidelidade())
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Cadastro realizado: usuarioId={}, perfil={}", salvo.getId(), salvo.getPerfil());

        String token = jwtTokenProvider.gerarToken(salvo.getEmail());

        return LoginResponseDTO.builder()
                .accessToken(token)
                .expiresIn(jwtExpiration)
                .id(salvo.getId())
                .nome(salvo.getNome())
                .email(salvo.getEmail())
                .perfil(salvo.getPerfil())
                .lojaId(salvo.getLoja() != null ? salvo.getLoja().getId() : null)
                .build();
    }
}
