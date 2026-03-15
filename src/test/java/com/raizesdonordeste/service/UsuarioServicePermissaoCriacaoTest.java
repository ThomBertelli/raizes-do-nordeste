package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.usuario.UsuarioCriacaoDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioRespostaDTO;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class UsuarioServicePermissaoCriacaoTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private final AtomicLong sequence = new AtomicLong(1L);

    @BeforeEach
    void setup() {
        when(passwordEncoder.encode(anyString())).thenReturn("senha-criptografada");
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0, Usuario.class);
            usuario.setId(sequence.getAndIncrement());
            return usuario;
        });
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, names = {"FUNCIONARIO", "GERENTE", "GERENCIA_MATRIZ"})
    @DisplayName("ADMIN pode criar FUNCIONARIO, GERENTE e GERENCIA_MATRIZ")
    void adminPodeCriarPerfisAdministrativos(PerfilUsuario perfilDesejado) {
        autenticarComo(PerfilUsuario.ADMIN);

        UsuarioCriacaoDTO dto = novoUsuarioDto(perfilDesejado, "admin-cria-" + perfilDesejado.name().toLowerCase() + "@mail.com");

        UsuarioRespostaDTO resposta = usuarioService.criar(dto);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getPerfil()).isEqualTo(perfilDesejado);
        assertThat(resposta.getEmail()).isEqualTo(dto.getEmail());
    }

    @Test
    @DisplayName("GERENTE pode criar usuario FUNCIONARIO")
    void gerentePodeCriarFuncionario() {
        autenticarComo(PerfilUsuario.GERENTE);

        UsuarioCriacaoDTO dto = novoUsuarioDto(PerfilUsuario.FUNCIONARIO, "gerente-cria-funcionario@mail.com");

        UsuarioRespostaDTO resposta = usuarioService.criar(dto);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getPerfil()).isEqualTo(PerfilUsuario.FUNCIONARIO);
        assertThat(resposta.getEmail()).isEqualTo("gerente-cria-funcionario@mail.com");
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, names = {"FUNCIONARIO", "GERENTE"})
    @DisplayName("GERENCIA_MATRIZ pode criar usuario FUNCIONARIO e GERENTE")
    void gerenciaMatrizPodeCriarFuncionarioEGerente(PerfilUsuario perfilDesejado) {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);

        UsuarioCriacaoDTO dto = novoUsuarioDto(
                perfilDesejado,
                "matriz-cria-" + perfilDesejado.name().toLowerCase() + "@mail.com"
        );

        UsuarioRespostaDTO resposta = usuarioService.criar(dto);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getPerfil()).isEqualTo(perfilDesejado);
        assertThat(resposta.getEmail()).isEqualTo(dto.getEmail());
    }

    private void autenticarComo(PerfilUsuario perfil) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("tester", "senha", List.of(() -> "ROLE_" + perfil.name()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private UsuarioCriacaoDTO novoUsuarioDto(PerfilUsuario perfil, String email) {
        return new UsuarioCriacaoDTO(
                "Usuario de Teste",
                email,
                "Senha@123",
                perfil,
                null
        );
    }
}

