package com.raizesdonordeste.service;

import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.MovimentacaoEstoque;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.MovimentacaoEstoqueRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private EstoqueRepository estoqueRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @InjectMocks
    private EstoqueService estoqueService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ deve acessar estoque da loja informada")
    void gerenciaMatrizDeveAcessarEstoqueDaLojaInformada() {
        autenticar(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Long lojaAutorizada = estoqueService.validarAcessoEstoque(10L);

        assertThat(lojaAutorizada).isEqualTo(10L);
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ sem lojaId deve acessar visão global")
    void gerenciaMatrizSemLojaIdDeveAcessarVisaoGlobal() {
        autenticar(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Long lojaAutorizada = estoqueService.validarAcessoEstoque(null);

        assertThat(lojaAutorizada).isNull();
    }

    @Test
    @DisplayName("GERENTE deve acessar apenas a própria loja")
    void gerenteDeveAcessarApenasAPropriaLoja() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Long lojaAutorizada = estoqueService.validarAcessoEstoque(null);

        assertThat(lojaAutorizada).isEqualTo(3L);
    }

    @Test
    @DisplayName("GERENTE não deve acessar loja diferente da sua")
    void gerenteNaoDeveAcessarLojaDiferenteDaSua() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        assertThatThrownBy(() -> estoqueService.validarAcessoEstoque(7L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outra loja");
    }

    @Test
    @DisplayName("Perfil sem permissão não deve acessar estoque")
    void perfilSemPermissaoNaoDeveAcessarEstoque() {
        autenticar(3L, "funcionario@teste.com", PerfilUsuario.FUNCIONARIO, 3L);

        assertThatThrownBy(() -> estoqueService.validarAcessoEstoque(3L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("sem permissão");
    }

    @Test
    @DisplayName("Deve listar estoques usando a loja resolvida pelo acesso")
    void deveListarEstoquesUsandoLojaResolvidaPeloAcesso() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Estoque> pagina = new PageImpl<>(List.of(), pageable, 0);
        when(estoqueRepository.findByLojaId(3L, pageable)).thenReturn(pagina);

        Page<Estoque> resposta = estoqueService.listarEstoquesPorLoja(null, pageable);

        assertThat(resposta).isNotNull();
        verify(estoqueRepository).findByLojaId(3L, pageable);
    }

    @Test
    @DisplayName("Deve listar movimentações filtrando por produto quando informado")
    void deveListarMovimentacoesFiltrandoPorProdutoQuandoInformado() {
        autenticar(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pageable pageable = PageRequest.of(0, 10);
        Page<MovimentacaoEstoque> pagina = new PageImpl<>(List.of(), pageable, 0);
        when(movimentacaoEstoqueRepository.findByLojaIdAndProdutoId(9L, 5L, pageable)).thenReturn(pagina);

        Page<MovimentacaoEstoque> resposta = estoqueService.listarMovimentacoesPorLoja(9L, 5L, pageable);

        assertThat(resposta).isNotNull();
        verify(movimentacaoEstoqueRepository).findByLojaIdAndProdutoId(9L, 5L, pageable);
        verify(movimentacaoEstoqueRepository, never()).findByLojaId(anyLong(), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ sem lojaId deve listar estoque de todas as lojas")
    void gerenciaMatrizSemLojaIdDeveListarEstoqueDeTodasAsLojas() {
        autenticar(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Estoque> pagina = new PageImpl<>(List.of(), pageable, 0);
        when(estoqueRepository.findAll(pageable)).thenReturn(pagina);

        Page<Estoque> resposta = estoqueService.listarEstoquesPorLoja(null, pageable);

        assertThat(resposta).isNotNull();
        verify(estoqueRepository).findAll(pageable);
        verify(estoqueRepository, never()).findByLojaId(anyLong(), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ sem lojaId deve listar todas movimentações")
    void gerenciaMatrizSemLojaIdDeveListarTodasMovimentacoes() {
        autenticar(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pageable pageable = PageRequest.of(0, 10);
        Page<MovimentacaoEstoque> pagina = new PageImpl<>(List.of(), pageable, 0);
        when(movimentacaoEstoqueRepository.findAll(pageable)).thenReturn(pagina);

        Page<MovimentacaoEstoque> resposta = estoqueService.listarMovimentacoesPorLoja(null, null, pageable);

        assertThat(resposta).isNotNull();
        verify(movimentacaoEstoqueRepository).findAll(pageable);
        verify(movimentacaoEstoqueRepository, never()).findByLojaId(anyLong(), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ sem lojaId deve filtrar movimentações por produto em todas as lojas")
    void gerenciaMatrizSemLojaIdDeveFiltrarMovimentacoesPorProdutoEmTodasAsLojas() {
        autenticar(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pageable pageable = PageRequest.of(0, 10);
        Page<MovimentacaoEstoque> pagina = new PageImpl<>(List.of(), pageable, 0);
        when(movimentacaoEstoqueRepository.findByProdutoId(5L, pageable)).thenReturn(pagina);

        Page<MovimentacaoEstoque> resposta = estoqueService.listarMovimentacoesPorLoja(null, 5L, pageable);

        assertThat(resposta).isNotNull();
        verify(movimentacaoEstoqueRepository).findByProdutoId(5L, pageable);
        verify(movimentacaoEstoqueRepository, never()).findByLojaIdAndProdutoId(anyLong(), anyLong(), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    private void autenticar(Long id, String email, PerfilUsuario perfil, Long lojaId) {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                id,
                lojaId,
                perfil,
                email,
                "senha",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

