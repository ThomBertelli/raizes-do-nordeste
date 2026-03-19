package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.loja.LojaUpdateDTO;
import com.raizesdonordeste.api.dto.loja.LojaCreateDTO;
import com.raizesdonordeste.api.dto.loja.LojaResponseDTO;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LojaServiceTest {

    @Mock
    private LojaRepository lojaRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private LojaService lojaService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Testes positivos — GERENCIA_MATRIZ pode fazer tudo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GERENCIA_MATRIZ pode criar loja")
    void gerenciaMatrizPodeCriarLoja() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        when(lojaRepository.existsByCnpj(anyString())).thenReturn(false);
        when(lojaRepository.save(any(Loja.class))).thenAnswer(inv -> {
            Loja l = inv.getArgument(0, Loja.class);
            l.setId(1L);
            return l;
        });

        LojaResponseDTO resposta = lojaService.criar(novaLojaCriacaoDTO());

        assertThat(resposta).isNotNull();
        assertThat(resposta.getNome()).isEqualTo("Loja Teste");
        assertThat(resposta.getCnpj()).isEqualTo("12.345.678/0001-90");
        verify(lojaRepository).save(any(Loja.class));
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ pode atualizar loja")
    void gerenciaMatrizPodeAtualizarLoja() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(lojaExistente()));
        when(lojaRepository.save(any(Loja.class))).thenAnswer(inv -> inv.getArgument(0, Loja.class));

        LojaUpdateDTO dto = new LojaUpdateDTO("Novo Nome", null, null, null);
        LojaResponseDTO resposta = lojaService.atualizar(1L, dto);

        assertThat(resposta.getNome()).isEqualTo("Novo Nome");
        verify(lojaRepository).save(any(Loja.class));
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ pode ativar loja")
    void gerenciaMatrizPodeAtivarLoja() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        Loja loja = lojaExistente();
        loja.setAtiva(false);
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(lojaRepository.save(any(Loja.class))).thenAnswer(inv -> inv.getArgument(0, Loja.class));

        lojaService.ativar(1L);

        assertThat(loja.isAtiva()).isTrue();
        verify(lojaRepository).save(loja);
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ pode desativar loja")
    void gerenciaMatrizPodeDesativarLoja() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        Loja loja = lojaExistente();
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(lojaRepository.save(any(Loja.class))).thenAnswer(inv -> inv.getArgument(0, Loja.class));

        lojaService.desativar(1L);

        assertThat(loja.isAtiva()).isFalse();
        verify(lojaRepository).save(loja);
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ pode deletar loja")
    void gerenciaMatrizPodeDeletarLoja() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        when(lojaRepository.existsById(1L)).thenReturn(true);

        lojaService.deletar(1L);

        verify(lojaRepository).deleteById(1L);
    }

    // -------------------------------------------------------------------------
    // Testes negativos — outros perfis NAO podem modificar lojas
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, mode = EnumSource.Mode.EXCLUDE, names = {"GERENCIA_MATRIZ"})
    @DisplayName("Somente GERENCIA_MATRIZ pode criar loja")
    void somenteGerenciaMatrizPodeCriarLoja(PerfilUsuario perfilSolicitante) {
        autenticarComo(perfilSolicitante);

        assertThatThrownBy(() -> lojaService.criar(novaLojaCriacaoDTO()))
                .isInstanceOf(AccessDeniedException.class);

        verify(lojaRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, mode = EnumSource.Mode.EXCLUDE, names = {"GERENCIA_MATRIZ"})
    @DisplayName("Somente GERENCIA_MATRIZ pode atualizar loja")
    void somenteGerenciaMatrizPodeAtualizarLoja(PerfilUsuario perfilSolicitante) {
        autenticarComo(perfilSolicitante);

        assertThatThrownBy(() -> lojaService.atualizar(1L, new LojaUpdateDTO("Novo Nome", null, null, null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(lojaRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, mode = EnumSource.Mode.EXCLUDE, names = {"GERENCIA_MATRIZ"})
    @DisplayName("Somente GERENCIA_MATRIZ pode ativar loja")
    void somenteGerenciaMatrizPodeAtivarLoja(PerfilUsuario perfilSolicitante) {
        autenticarComo(perfilSolicitante);

        assertThatThrownBy(() -> lojaService.ativar(1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(lojaRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, mode = EnumSource.Mode.EXCLUDE, names = {"GERENCIA_MATRIZ"})
    @DisplayName("Somente GERENCIA_MATRIZ pode desativar loja")
    void somenteGerenciaMatrizPodeDesativarLoja(PerfilUsuario perfilSolicitante) {
        autenticarComo(perfilSolicitante);

        assertThatThrownBy(() -> lojaService.desativar(1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(lojaRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, mode = EnumSource.Mode.EXCLUDE, names = {"GERENCIA_MATRIZ"})
    @DisplayName("Somente GERENCIA_MATRIZ pode deletar loja")
    void somenteGerenciaMatrizPodeDeletarLoja(PerfilUsuario perfilSolicitante) {
        autenticarComo(perfilSolicitante);

        assertThatThrownBy(() -> lojaService.deletar(1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(lojaRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Usuario nao autenticado nao pode criar loja")
    void usuarioNaoAutenticadoNaoPodeCriarLoja() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> lojaService.criar(novaLojaCriacaoDTO()))
                .isInstanceOf(AccessDeniedException.class);

        verify(lojaRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Testes de integridade dos dados
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Nao deve criar loja com CNPJ ja cadastrado")
    void naoDeveCriarLojaComCnpjDuplicado() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        when(lojaRepository.existsByCnpj("12.345.678/0001-90")).thenReturn(true);

        assertThatThrownBy(() -> lojaService.criar(novaLojaCriacaoDTO()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CNPJ");

        verify(lojaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve atualizar loja com CNPJ ja usado por outra loja")
    void naoDeveAtualizarLojaComCnpjDuplicado() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        Loja loja = lojaExistente();
        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(lojaRepository.existsByCnpj("99.999.999/0001-99")).thenReturn(true);

        LojaUpdateDTO dto = new LojaUpdateDTO(null, "99.999.999/0001-99", null, null);

        assertThatThrownBy(() -> lojaService.atualizar(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CNPJ");

        verify(lojaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao ao buscar loja inexistente")
    void deveLancarExcecaoAoBuscarLojaInexistente() {
        when(lojaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lojaService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar ativar loja inexistente")
    void deveLancarExcecaoAoAtivarLojaInexistente() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        when(lojaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lojaService.ativar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar desativar loja inexistente")
    void deveLancarExcecaoAoDesativarLojaInexistente() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        when(lojaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lojaService.desativar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar deletar loja inexistente")
    void deveLancarExcecaoAoDeletarLojaInexistente() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        when(lojaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> lojaService.deletar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Loja deve ser criada como ativa por padrao")
    void lojaDeveSerCriadaComoAtivaPorPadrao() {
        autenticarComo(PerfilUsuario.GERENCIA_MATRIZ);
        when(lojaRepository.existsByCnpj(anyString())).thenReturn(false);
        when(lojaRepository.save(any(Loja.class))).thenAnswer(inv -> {
            Loja l = inv.getArgument(0, Loja.class);
            l.setId(1L);
            return l;
        });

        LojaResponseDTO resposta = lojaService.criar(novaLojaCriacaoDTO());

        assertThat(resposta.isAtiva()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void autenticarComo(PerfilUsuario perfil) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("tester", "senha", List.of(() -> "ROLE_" + perfil.name()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(securityContextService.getRequiredPerfil()).thenReturn(perfil);
        lenient().when(securityContextService.getActorIdOrNull()).thenReturn(null);
        lenient().when(securityContextService.getActorPerfilOrNull()).thenReturn(perfil);
    }

    private LojaCreateDTO novaLojaCriacaoDTO() {
        return new LojaCreateDTO("Loja Teste", "12.345.678/0001-90", "Rua das Flores, 123");
    }

    private Loja lojaExistente() {
        return Loja.builder()
                .id(1L)
                .nome("Loja Original")
                .cnpj("12.345.678/0001-90")
                .endereco("Rua das Flores, 123")
                .ativa(true)
                .build();
    }
}

