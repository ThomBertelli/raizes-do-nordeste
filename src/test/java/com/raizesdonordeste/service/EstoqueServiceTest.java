package com.raizesdonordeste.service;

import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.enums.TipoMovimentacaoEstoque;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.MovimentacaoEstoque;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.domain.repository.MovimentacaoEstoqueRepository;
import com.raizesdonordeste.domain.repository.ProdutoRepository;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private EstoqueRepository estoqueRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Mock
    private LojaRepository lojaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityContextService securityContextService;

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

    @Test
    @DisplayName("GERENTE deve registrar entrada de estoque e movimentação")
    void gerenteDeveRegistrarEntradaDeEstoqueEMovimentacao() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Estoque estoque = estoqueExemplo(3L, 10L, 8);
        Usuario usuario = usuarioExemplo(2L, "Gerente Loja", PerfilUsuario.GERENTE);

        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(3L, 10L)).thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.getReferenceById(2L)).thenReturn(usuario);

        Estoque resposta = estoqueService.registrarEntrada(null, 10L, 4, "Reposição");

        assertThat(resposta.getQuantidade()).isEqualTo(12);
        verify(estoqueRepository).findByLojaIdAndProdutoIdWithLock(3L, 10L);
        verify(movimentacaoEstoqueRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("GERENTE deve registrar saída de estoque e movimentação")
    void gerenteDeveRegistrarSaidaDeEstoqueEMovimentacao() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Estoque estoque = estoqueExemplo(3L, 10L, 12);
        Usuario usuario = usuarioExemplo(2L, "Gerente Loja", PerfilUsuario.GERENTE);

        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(3L, 10L)).thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.getReferenceById(2L)).thenReturn(usuario);

        Estoque resposta = estoqueService.registrarSaida(null, 10L, 5, "Venda balcão");

        assertThat(resposta.getQuantidade()).isEqualTo(7);
        verify(estoqueRepository).findByLojaIdAndProdutoIdWithLock(3L, 10L);
        verify(movimentacaoEstoqueRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("Não deve registrar saída quando saldo for insuficiente")
    void naoDeveRegistrarSaidaQuandoSaldoForInsuficiente() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Estoque estoque = estoqueExemplo(3L, 10L, 2);
        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(3L, 10L)).thenReturn(Optional.of(estoque));

        assertThatThrownBy(() -> estoqueService.registrarSaida(null, 10L, 5, "Ajuste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(estoqueRepository, never()).save(any(Estoque.class));
        verify(movimentacaoEstoqueRepository, never()).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("GERENTE não deve registrar movimentação em outra loja")
    void gerenteNaoDeveRegistrarMovimentacaoEmOutraLoja() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        assertThatThrownBy(() -> estoqueService.registrarEntrada(8L, 10L, 3, "Reposição"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outra loja");

        verify(estoqueRepository, never()).findByLojaIdAndProdutoIdWithLock(anyLong(), anyLong());
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ deve registrar entrada na loja informada")
    void gerenciaMatrizDeveRegistrarEntradaNaLojaInformada() {
        autenticar(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Estoque estoque = estoqueExemplo(9L, 10L, 1);
        Usuario usuario = usuarioExemplo(1L, "Matriz", PerfilUsuario.GERENCIA_MATRIZ);

        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(9L, 10L)).thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.getReferenceById(1L)).thenReturn(usuario);

        Estoque resposta = estoqueService.registrarEntrada(9L, 10L, 2, "Abastecimento central");

        assertThat(resposta.getQuantidade()).isEqualTo(3);
        verify(movimentacaoEstoqueRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("Deve criar estoque inicial quando entrada ocorrer sem registro prévio")
    void deveCriarEstoqueInicialQuandoEntradaOcorrerSemRegistroPrevio() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Loja loja = Loja.builder().id(3L).nome("Loja Teste").cnpj("00.000.000/0001-00").endereco("Rua A, 1").ativa(true).build();
        Produto produto = Produto.builder().id(10L).nome("Produto").descricao("Desc").build();
        Usuario usuario = usuarioExemplo(2L, "Gerente Loja", PerfilUsuario.GERENTE);

        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(3L, 10L)).thenReturn(Optional.empty());
        when(lojaRepository.findById(3L)).thenReturn(Optional.of(loja));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.getReferenceById(2L)).thenReturn(usuario);

        Estoque resposta = estoqueService.registrarEntrada(null, 10L, 6, "Primeira carga");

        assertThat(resposta.getQuantidade()).isEqualTo(6);
        verify(estoqueRepository, times(2)).save(any(Estoque.class));
        verify(movimentacaoEstoqueRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("Deve recuperar estoque existente quando ocorrer conflito de concorrência na criação inicial")
    void deveRecuperarEstoqueExistenteQuandoOcorrerConflitoDeConcorrenciaNaCriacaoInicial() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Loja loja = Loja.builder().id(3L).nome("Loja Teste").cnpj("00.000.000/0001-00").endereco("Rua A, 1").ativa(true).build();
        Produto produto = Produto.builder().id(10L).nome("Produto").descricao("Desc").build();
        Usuario usuario = usuarioExemplo(2L, "Gerente Loja", PerfilUsuario.GERENTE);
        Estoque estoqueConcorrente = estoqueExemplo(3L, 10L, 5);

        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(3L, 10L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(estoqueConcorrente));
        when(lojaRepository.findById(3L)).thenReturn(Optional.of(loja));
        when(produtoRepository.findById(10L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.save(any(Estoque.class)))
                .thenThrow(new DataIntegrityViolationException("uk_estoques_loja_produto"))
                .thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.getReferenceById(2L)).thenReturn(usuario);

        Estoque resposta = estoqueService.registrarEntrada(null, 10L, 3, "Reposição");

        assertThat(resposta.getQuantidade()).isEqualTo(8);
        verify(estoqueRepository, times(2)).findByLojaIdAndProdutoIdWithLock(3L, 10L);
        verify(estoqueRepository, times(2)).save(any(Estoque.class));
        verify(movimentacaoEstoqueRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("Deve falhar saída quando estoque não existir")
    void deveFalharSaidaQuandoEstoqueNaoExistir() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);
        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(3L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoqueService.registrarSaida(null, 10L, 2, "Venda"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Estoque não encontrado");

        verify(movimentacaoEstoqueRepository, never()).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("Deve falhar movimentação com quantidade inválida")
    void deveFalharMovimentacaoComQuantidadeInvalida() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        assertThatThrownBy(() -> estoqueService.registrarEntrada(null, 10L, 0, "Reposição"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantidade deve ser maior que zero");

        verify(estoqueRepository, never()).findByLojaIdAndProdutoIdWithLock(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Movimentação de entrada deve ser registrada com tipo ENTRADA")
    void movimentacaoDeEntradaDeveSerRegistradaComTipoEntrada() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Estoque estoque = estoqueExemplo(3L, 10L, 4);
        Usuario usuario = usuarioExemplo(2L, "Gerente Loja", PerfilUsuario.GERENTE);

        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(3L, 10L)).thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.getReferenceById(2L)).thenReturn(usuario);

        estoqueService.registrarEntrada(null, 10L, 1, "Reposição");

        verify(movimentacaoEstoqueRepository).save(org.mockito.ArgumentMatchers.argThat(movimentacao ->
                movimentacao.getTipo() == TipoMovimentacaoEstoque.ENTRADA
                        && movimentacao.getQuantidade().equals(1)
                        && "Reposição".equals(movimentacao.getMotivo())
        ));
    }

    @Test
    @DisplayName("Movimentação de saída deve ser registrada com tipo SAIDA")
    void movimentacaoDeSaidaDeveSerRegistradaComTipoSaida() {
        autenticar(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Estoque estoque = estoqueExemplo(3L, 10L, 4);
        Usuario usuario = usuarioExemplo(2L, "Gerente Loja", PerfilUsuario.GERENTE);

        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(3L, 10L)).thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.getReferenceById(2L)).thenReturn(usuario);

        estoqueService.registrarSaida(null, 10L, 1, "Venda");

        verify(movimentacaoEstoqueRepository).save(org.mockito.ArgumentMatchers.argThat(movimentacao ->
                movimentacao.getTipo() == TipoMovimentacaoEstoque.SAIDA
                        && movimentacao.getQuantidade().equals(1)
                        && "Venda".equals(movimentacao.getMotivo())
        ));
    }

    private Estoque estoqueExemplo(Long lojaId, Long produtoId, Integer quantidade) {
        Loja loja = Loja.builder().id(lojaId).nome("Loja Teste").cnpj("00.000.000/0001-00").endereco("Rua A, 1").ativa(true).build();
        Produto produto = Produto.builder().id(produtoId).nome("Produto").descricao("Desc").build();

        return Estoque.builder()
                .id(100L)
                .loja(loja)
                .produto(produto)
                .quantidade(quantidade)
                .versao(1L)
                .build();
    }

    private Usuario usuarioExemplo(Long id, String nome, PerfilUsuario perfil) {
        return Usuario.builder()
                .id(id)
                .nome(nome)
                .email("usuario@teste.com")
                .senha("senha")
                .perfil(perfil)
                .ativo(true)
                .consentimentoProgramaFidelidade(false)
                .build();
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
        lenient().when(securityContextService.getRequiredPrincipal()).thenReturn(principal);
        lenient().when(securityContextService.getRequiredPerfil()).thenReturn(perfil);
    }
}

