package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pedido.PedidoItemRequestDTO;
import com.raizesdonordeste.api.dto.pedido.PedidoRequestDTO;
import com.raizesdonordeste.api.dto.pedido.PedidoResponseDTO;
import com.raizesdonordeste.api.dto.pedido.PedidoStatusUpdateDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.domain.repository.PedidoRepository;
import com.raizesdonordeste.domain.repository.ProdutoRepository;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.exception.RegraNegocioException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private LojaRepository lojaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityContextService securityContextService;

    @Spy
    private PedidoAuthorization pedidoAuthorization;

    @InjectMocks
    private PedidoService pedidoService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Testes de listagem por loja
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GERENCIA_MATRIZ deve listar todos os pedidos")
    void gerenciaMatrizDeveListarTodosOsPedidos() {
        autenticarComo(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pedido pedido1 = pedidoExemplo(1L, lojaExemplo(1L), clienteExemplo(10L));
        Pedido pedido2 = pedidoExemplo(2L, lojaExemplo(2L), clienteExemplo(11L));
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findAllWithRelacionamentos(pageable))
                .thenReturn(new PageImpl<>(List.of(pedido1, pedido2), pageable, 2));

        Page<PedidoResponseDTO> resultado = pedidoService.listarPorLoja(null, null, null, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        verify(pedidoRepository).findAllWithRelacionamentos(pageable);
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ ignora lojaId solicitado")
    void gerenciaMatrizIgnoraLojaIdSolicitado() {
        autenticarComo(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findAllWithRelacionamentos(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        pedidoService.listarPorLoja(999L, null, null, pageable);

        verify(pedidoRepository).findAllWithRelacionamentos(pageable);
    }

    @Test
    @DisplayName("GERENTE deve listar apenas pedidos da sua loja")
    void gerenteDeveListarApenasPedidosDaSuaLoja() {
        autenticarComo(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(3L), clienteExemplo(10L));
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByLojaIdOrderByDataCriacaoDescComRelacionamentos(3L, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoResponseDTO> resultado = pedidoService.listarPorLoja(null, null, null, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getLojaId()).isEqualTo(3L);
        verify(pedidoRepository).findByLojaIdOrderByDataCriacaoDescComRelacionamentos(3L, pageable);
    }

    @Test
    @DisplayName("GERENTE deve listar quando lojaId solicitado for sua loja")
    void gerenteDeveListarQuandoLojaIdSolicitadoForSuaLoja() {
        autenticarComo(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByLojaIdOrderByDataCriacaoDescComRelacionamentos(3L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        pedidoService.listarPorLoja(3L, null, null, pageable);

        verify(pedidoRepository).findByLojaIdOrderByDataCriacaoDescComRelacionamentos(3L, pageable);
    }

    @Test
    @DisplayName("GERENTE nao deve acessar loja diferente da sua")
    void gerenteNaoDeveAcessarLojaDiferenteDaSua() {
        autenticarComo(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        assertThatThrownBy(() -> pedidoService.listarPorLoja(7L, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outra loja");
        verify(pedidoRepository, never()).findAllWithRelacionamentos(any(Pageable.class));
        verify(pedidoRepository, never()).findByLojaIdOrderByDataCriacaoDescComRelacionamentos(anyLong(), any(Pageable.class));
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, mode = EnumSource.Mode.EXCLUDE, names = {"GERENCIA_MATRIZ", "GERENTE"})
    @DisplayName("Apenas GERENCIA_MATRIZ e GERENTE podem listar por loja")
    void apenasGerenciaMatrizEGerentePodemListarPorLoja(PerfilUsuario perfilSemPermissao) {
        autenticarComo(3L, "usuario@teste.com", perfilSemPermissao, null);

        assertThatThrownBy(() -> pedidoService.listarPorLoja(null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não autorizado");
        verify(pedidoRepository, never()).findAllWithRelacionamentos(any(Pageable.class));
        verify(pedidoRepository, never()).findByLojaIdOrderByDataCriacaoDescComRelacionamentos(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("Usuario nao autenticado nao pode listar pedidos por loja")
    void usuarioNaoAutenticadoNaoPodeListarPedidosPorLoja() {
        SecurityContextHolder.clearContext();
        when(securityContextService.getRequiredPrincipal())
                .thenThrow(new AccessDeniedException("Usuário não autenticado"));

        assertThatThrownBy(() -> pedidoService.listarPorLoja(null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
        verify(pedidoRepository, never()).findAllWithRelacionamentos(any(Pageable.class));
        verify(pedidoRepository, never()).findByLojaIdOrderByDataCriacaoDescComRelacionamentos(anyLong(), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // Testes de criacao de pedido
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CLIENTE pode criar pedido")
    void clientePodeCriarPedido() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.APP, List.of(
                new PedidoItemRequestDTO(5L, 2)
        ));

        Loja loja = lojaExemplo(1L);
        Produto produto = produtoExemplo(5L, new BigDecimal("10.00"));
        Usuario cliente = clienteExemplo(10L);
        Estoque estoque = estoqueExemplo(loja, produto, 5);

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(5L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(1L, 5L))
                .thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0, Estoque.class));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido pedido = inv.getArgument(0, Pedido.class);
            pedido.setId(99L);
            return pedido;
        });

        PedidoResponseDTO resposta = pedidoService.criar(request);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getStatusPedido()).isEqualTo(StatusPedido.CRIADO);
        assertThat(resposta.getValorTotal()).isEqualTo(new BigDecimal("20.00"));
        assertThat(estoque.getQuantidade()).isEqualTo(3);
        verify(estoqueRepository).save(any(Estoque.class));
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    @DisplayName("FUNCIONARIO pode criar pedido")
    void funcionarioPodeCriarPedido() {
        autenticarComo(20L, "func@teste.com", PerfilUsuario.FUNCIONARIO, 1L);

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.BALCAO, List.of(
                new PedidoItemRequestDTO(5L, 1)
        ));

        Loja loja = lojaExemplo(1L);
        Produto produto = produtoExemplo(5L, new BigDecimal("10.00"));
        Usuario funcionario = clienteExemplo(20L);
        Estoque estoque = estoqueExemplo(loja, produto, 5);

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(funcionario));
        when(produtoRepository.findById(5L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(1L, 5L))
                .thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0, Estoque.class));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0, Pedido.class));

        PedidoResponseDTO resposta = pedidoService.criar(request);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getStatusPedido()).isEqualTo(StatusPedido.CRIADO);
        assertThat(resposta.getClienteId()).isEqualTo(20L);
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    @DisplayName("GERENTE pode criar pedido")
    void gerentePodeCriarPedido() {
        autenticarComo(30L, "gerente@teste.com", PerfilUsuario.GERENTE, 1L);

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.BALCAO, List.of(
                new PedidoItemRequestDTO(5L, 1)
        ));

        Loja loja = lojaExemplo(1L);
        Produto produto = produtoExemplo(5L, new BigDecimal("10.00"));
        Usuario gerente = clienteExemplo(30L);
        Estoque estoque = estoqueExemplo(loja, produto, 5);

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(usuarioRepository.findById(30L)).thenReturn(Optional.of(gerente));
        when(produtoRepository.findById(5L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(1L, 5L))
                .thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0, Estoque.class));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0, Pedido.class));

        PedidoResponseDTO resposta = pedidoService.criar(request);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getStatusPedido()).isEqualTo(StatusPedido.CRIADO);
        assertThat(resposta.getClienteId()).isEqualTo(30L);
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Concorrencia: apenas um pedido criado quando estoque so atende uma requisicao")
    void concorrenciaDeEstoqueNaoPermiteDoisPedidos() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.APP, List.of(
                new PedidoItemRequestDTO(5L, 2)
        ));

        Loja loja = lojaExemplo(1L);
        Produto produto = produtoExemplo(5L, new BigDecimal("10.00"));
        Usuario cliente = clienteExemplo(10L);
        Estoque estoque = estoqueExemplo(loja, produto, 2);

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById(5L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(1L, 5L))
                .thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(inv -> inv.getArgument(0, Estoque.class));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido pedido = inv.getArgument(0, Pedido.class);
            pedido.setId(100L);
            return pedido;
        });

        PedidoResponseDTO primeiraResposta = pedidoService.criar(request);

        assertThat(primeiraResposta).isNotNull();
        assertThat(estoque.getQuantidade()).isEqualTo(0);

        assertThatThrownBy(() -> pedidoService.criar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");

        verify(pedidoRepository, times(1)).save(any(Pedido.class));
        verify(estoqueRepository, times(1)).save(any(Estoque.class));
    }

    @Test
    @DisplayName("Nao deve criar pedido sem canalPedido")
    void naoDeveCriarPedidoSemCanalPedido() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        PedidoRequestDTO request = novoPedidoRequest(1L, null, List.of(
                new PedidoItemRequestDTO(5L, 1)
        ));

        assertThatThrownBy(() -> pedidoService.criar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("canalPedido");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Nao deve criar pedido sem itens")
    void naoDeveCriarPedidoSemItens() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.APP, List.of());

        assertThatThrownBy(() -> pedidoService.criar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("itens");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Nao deve criar pedido com loja inexistente")
    void naoDeveCriarPedidoComLojaInexistente() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.APP, List.of(
                new PedidoItemRequestDTO(5L, 1)
        ));

        when(lojaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.criar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Loja não encontrada");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Nao deve criar pedido com produto inexistente")
    void naoDeveCriarPedidoComProdutoInexistente() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.APP, List.of(
                new PedidoItemRequestDTO(5L, 1)
        ));

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(lojaExemplo(1L)));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(clienteExemplo(10L)));
        when(produtoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.criar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Produto não encontrado");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Nao deve criar pedido com estoque insuficiente")
    void naoDeveCriarPedidoComEstoqueInsuficiente() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.APP, List.of(
                new PedidoItemRequestDTO(5L, 3)
        ));

        Loja loja = lojaExemplo(1L);
        Produto produto = produtoExemplo(5L, new BigDecimal("10.00"));

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(clienteExemplo(10L)));
        when(produtoRepository.findById(5L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.findByLojaIdAndProdutoIdWithLock(1L, 5L))
                .thenReturn(Optional.of(estoqueExemplo(loja, produto, 2)));

        assertThatThrownBy(() -> pedidoService.criar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Nao deve criar pedido sem autenticacao")
    void naoDeveCriarPedidoSemAutenticacao() {
        SecurityContextHolder.clearContext();
        when(securityContextService.getRequiredPrincipal())
                .thenThrow(new AccessDeniedException("Usuário não autenticado"));

        PedidoRequestDTO request = novoPedidoRequest(1L, CanalPedido.APP, List.of(
                new PedidoItemRequestDTO(5L, 1)
        ));

        assertThatThrownBy(() -> pedidoService.criar(request))
                .isInstanceOf(AccessDeniedException.class);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // -------------------------------------------------------------------------
    // Testes de listagem dos pedidos do cliente
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CLIENTE deve listar apenas seus pedidos")
    void clienteDeveListarApenasSeusPedidos() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(1L), clienteExemplo(10L));
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByClienteIdOrderByDataCriacaoDescComRelacionamentos(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoResponseDTO> resultado = pedidoService.listarMeusPedidos(pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getClienteId()).isEqualTo(10L);
        verify(pedidoRepository).findByClienteIdOrderByDataCriacaoDescComRelacionamentos(10L, pageable);
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, mode = EnumSource.Mode.EXCLUDE, names = {"CLIENTE"})
    @DisplayName("Apenas CLIENTE pode listar seus pedidos")
    void apenasClientePodeListarSeusPedidos(PerfilUsuario perfilNaoCliente) {
        Long lojaId = perfilNaoCliente.equals(PerfilUsuario.GERENTE) ? 1L : null;
        autenticarComo(2L, "usuario@teste.com", perfilNaoCliente, lojaId);

        assertThatThrownBy(() -> pedidoService.listarMeusPedidos(PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Apenas clientes");
        verify(pedidoRepository, never()).findByClienteIdOrderByDataCriacaoDescComRelacionamentos(anyLong(), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // Testes de busca por id
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GERENCIA_MATRIZ deve buscar qualquer pedido por ID")
    void gerenciaMatrizDeveBuscarQualquerPedidoPorId() {
        autenticarComo(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(2L), clienteExemplo(10L));
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        PedidoResponseDTO resultado = pedidoService.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(pedidoRepository).findByIdWithRelacionamentos(1L);
    }

    @Test
    @DisplayName("GERENTE deve buscar pedido da sua loja")
    void gerenteDeveBuscarPedidoDaSuaLoja() {
        autenticarComo(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(3L), clienteExemplo(10L));
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        PedidoResponseDTO resultado = pedidoService.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(pedidoRepository).findByIdWithRelacionamentos(1L);
    }

    @Test
    @DisplayName("GERENTE nao deve buscar pedido de outra loja")
    void gerenteNaoDeveBuscarPedidoDeOutraLoja() {
        autenticarComo(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(7L), clienteExemplo(10L));
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.buscarPorId(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outra loja");
    }

    @Test
    @DisplayName("CLIENTE deve buscar seu proprio pedido")
    void clienteDeveBuscarSeuProprioPedido() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(1L), clienteExemplo(10L));
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        PedidoResponseDTO resultado = pedidoService.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(pedidoRepository).findByIdWithRelacionamentos(1L);
    }

    @Test
    @DisplayName("CLIENTE nao deve buscar pedido de outro cliente")
    void clienteNaoDeveBuscarPedidoDeOutroCliente() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(1L), clienteExemplo(20L));
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.buscarPorId(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outro cliente");
    }

    @Test
    @DisplayName("Deve lancar excecao ao buscar pedido inexistente")
    void deveLancarExcecaoAoBuscarPedidoInexistente() {
        autenticarComo(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);
        when(pedidoRepository.findByIdWithRelacionamentos(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.buscarPorId(999L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Pedido não encontrado");
    }

    // -------------------------------------------------------------------------
    // Testes de atualizacao de status do pedido (operacao loja)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("FUNCIONARIO pode avancar status CONFIRMADO -> PREPARO na propria loja")
    void funcionarioPodeAvancarStatusConfirmadoParaPreparoNaPropriaLoja() {
        autenticarComo(20L, "func@teste.com", PerfilUsuario.FUNCIONARIO, 5L);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(5L), clienteExemplo(10L));
        pedido.setStatusPedido(StatusPedido.CONFIRMADO);
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0, Pedido.class));

        PedidoStatusUpdateDTO request = new PedidoStatusUpdateDTO(
                StatusPedido.PREPARO,
                PedidoStatusUpdateDTO.OrigemStatusPedido.OPERACAO_LOJA
        );

        PedidoResponseDTO resultado = pedidoService.atualizarStatusOperacaoLoja(1L, request);

        assertThat(resultado.getStatusPedido()).isEqualTo(StatusPedido.PREPARO);
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Nao permite transicao invalida de status na operacao da loja")
    void naoPermiteTransicaoInvalidaNaOperacaoLoja() {
        autenticarComo(20L, "func@teste.com", PerfilUsuario.FUNCIONARIO, 5L);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(5L), clienteExemplo(10L));
        pedido.setStatusPedido(StatusPedido.CRIADO);
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        PedidoStatusUpdateDTO request = new PedidoStatusUpdateDTO(
                StatusPedido.PREPARO,
                PedidoStatusUpdateDTO.OrigemStatusPedido.OPERACAO_LOJA
        );

        assertThatThrownBy(() -> pedidoService.atualizarStatusOperacaoLoja(1L, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Transição de status não permitida");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Cliente nao pode atualizar status de pedido")
    void clienteNaoPodeAtualizarStatusPedido() {
        autenticarComo(10L, "cliente@teste.com", PerfilUsuario.CLIENTE, null);

        PedidoStatusUpdateDTO request = new PedidoStatusUpdateDTO(
                StatusPedido.PREPARO,
                PedidoStatusUpdateDTO.OrigemStatusPedido.OPERACAO_LOJA
        );

        assertThatThrownBy(() -> pedidoService.atualizarStatusOperacaoLoja(1L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Perfil não autorizado");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Funcionario nao pode atualizar pedido de outra loja")
    void funcionarioNaoPodeAtualizarPedidoDeOutraLoja() {
        autenticarComo(20L, "func@teste.com", PerfilUsuario.FUNCIONARIO, 5L);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(8L), clienteExemplo(10L));
        pedido.setStatusPedido(StatusPedido.CONFIRMADO);
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        PedidoStatusUpdateDTO request = new PedidoStatusUpdateDTO(
                StatusPedido.PREPARO,
                PedidoStatusUpdateDTO.OrigemStatusPedido.OPERACAO_LOJA
        );

        assertThatThrownBy(() -> pedidoService.atualizarStatusOperacaoLoja(1L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("outra loja");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Origem diferente de OPERACAO_LOJA nao e aceita")
    void origemDiferenteDeOperacaoLojaNaoEAceita() {
        autenticarComo(20L, "func@teste.com", PerfilUsuario.FUNCIONARIO, 5L);

        PedidoStatusUpdateDTO request = new PedidoStatusUpdateDTO(
                StatusPedido.PREPARO,
                PedidoStatusUpdateDTO.OrigemStatusPedido.PAGAMENTO
        );

        assertThatThrownBy(() -> pedidoService.atualizarStatusOperacaoLoja(1L, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Origem inválida");
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Pedido pedidoExemplo(Long id, Loja loja, Usuario cliente) {
        return Pedido.builder()
                .id(id)
                .loja(loja)
                .cliente(cliente)
                .canalPedido(CanalPedido.APP)
                .statusPedido(StatusPedido.CRIADO)
                .valorTotal(BigDecimal.valueOf(100.00))
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
                .build();
    }

    private Produto produtoExemplo(Long id, BigDecimal preco) {
        return Produto.builder()
                .id(id)
                .nome("Produto " + id)
                .descricao("Descricao")
                .preco(preco)
                .ativo(true)
                .build();
    }

    private Estoque estoqueExemplo(Loja loja, Produto produto, Integer quantidade) {
        return Estoque.builder()
                .id(1L)
                .loja(loja)
                .produto(produto)
                .quantidade(quantidade)
                .build();
    }

    private Loja lojaExemplo(Long id) {
        return Loja.builder()
                .id(id)
                .nome("Loja " + id)
                .cnpj(String.format("%014d", id))
                .endereco("Rua Teste, 123")
                .ativa(true)
                .build();
    }

    private Usuario clienteExemplo(Long id) {
        return Usuario.builder()
                .id(id)
                .nome("Cliente " + id)
                .email("cliente" + id + "@teste.com")
                .senha("senha_hash")
                .perfil(PerfilUsuario.CLIENTE)
                .ativo(true)
                .consentimentoProgramaFidelidade(false)
                .build();
    }

    private void autenticarComo(Long id, String email, PerfilUsuario perfil, Long lojaId) {
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

    private PedidoRequestDTO novoPedidoRequest(Long lojaId, CanalPedido canalPedido, List<PedidoItemRequestDTO> itens) {
        return new PedidoRequestDTO(lojaId, canalPedido, itens);
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ filtra por canalPedido")
    void gerenciaMatrizFiltraPorCanalPedido() {
        autenticarComo(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(1L), clienteExemplo(10L));
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByCanalPedidoOrderByDataCriacaoDescComRelacionamentos(CanalPedido.APP, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoResponseDTO> resultado = pedidoService.listarPorLoja(null, CanalPedido.APP, null, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(pedidoRepository).findByCanalPedidoOrderByDataCriacaoDescComRelacionamentos(CanalPedido.APP, pageable);
    }

    @Test
    @DisplayName("GERENCIA_MATRIZ filtra por statusPedido")
    void gerenciaMatrizFiltraPorStatusPedido() {
        autenticarComo(1L, "matriz@teste.com", PerfilUsuario.GERENCIA_MATRIZ, null);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(1L), clienteExemplo(10L));
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByStatusPedidoOrderByDataCriacaoDescComRelacionamentos(StatusPedido.CRIADO, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoResponseDTO> resultado = pedidoService.listarPorLoja(null, null, StatusPedido.CRIADO, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(pedidoRepository).findByStatusPedidoOrderByDataCriacaoDescComRelacionamentos(StatusPedido.CRIADO, pageable);
    }

    @Test
    @DisplayName("GERENTE filtra por canal e status da propria loja")
    void gerenteFiltraPorCanalEStatusDaPropriaLoja() {
        autenticarComo(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(3L), clienteExemplo(10L));
        Pageable pageable = PageRequest.of(0, 10);
        when(pedidoRepository.findByLojaIdAndCanalPedidoAndStatusPedidoOrderByDataCriacaoDescComRelacionamentos(
                3L, CanalPedido.APP, StatusPedido.CRIADO, pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoResponseDTO> resultado = pedidoService.listarPorLoja(3L, CanalPedido.APP, StatusPedido.CRIADO, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getLojaId()).isEqualTo(3L);
        verify(pedidoRepository).findByLojaIdAndCanalPedidoAndStatusPedidoOrderByDataCriacaoDescComRelacionamentos(
                3L, CanalPedido.APP, StatusPedido.CRIADO, pageable);
    }
}
