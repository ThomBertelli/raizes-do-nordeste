package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.auth.LoginRequestDTO;
import com.raizesdonordeste.api.dto.auth.LoginResponseDTO;
import com.raizesdonordeste.config.JwtTokenProvider;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Login deve retornar lojaId para gerente vinculado")
    void loginDeveRetornarLojaIdParaGerenteVinculado() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 3600L);

        LoginRequestDTO request = new LoginRequestDTO("gerente@teste.com", "Senha@123");
        Authentication authentication = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha());

        Loja loja = Loja.builder()
                .id(7L)
                .nome("Loja Centro")
                .cnpj("12.345.678/0001-90")
                .endereco("Rua A, 10")
                .ativa(true)
                .build();

        Usuario usuario = Usuario.builder()
                .id(10L)
                .nome("Gerente Centro")
                .email(request.getEmail())
                .senha("hash")
                .perfil(PerfilUsuario.GERENTE)
                .loja(loja)
                .ativo(true)
                .consentimentoProgramaFidelidade(false)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtTokenProvider.gerarToken(authentication)).thenReturn("jwt-token");
        when(usuarioRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(usuario));

        LoginResponseDTO resposta = authService.autenticar(request);

        assertThat(resposta.getAccessToken()).isEqualTo("jwt-token");
        assertThat(resposta.getPerfil()).isEqualTo(PerfilUsuario.GERENTE);
        assertThat(resposta.getLojaId()).isEqualTo(7L);
    }
}
