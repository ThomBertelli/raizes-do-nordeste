package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pedido.PedidoRespostaDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.PedidoRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

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

        Page<PedidoRespostaDTO> resultado = pedidoService.listarPorLoja(null, pageable);

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

        pedidoService.listarPorLoja(999L, pageable);

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

        Page<PedidoRespostaDTO> resultado = pedidoService.listarPorLoja(null, pageable);

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

        pedidoService.listarPorLoja(3L, pageable);

        verify(pedidoRepository).findByLojaIdOrderByDataCriacaoDescComRelacionamentos(3L, pageable);
    }

    @Test
    @DisplayName("GERENTE nao deve acessar loja diferente da sua")
    void gerenteNaoDeveAcessarLojaDiferenteDaSua() {
        autenticarComo(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        assertThatThrownBy(() -> pedidoService.listarPorLoja(7L, PageRequest.of(0, 10)))
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

        assertThatThrownBy(() -> pedidoService.listarPorLoja(null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não autorizado");
        verify(pedidoRepository, never()).findAllWithRelacionamentos(any(Pageable.class));
        verify(pedidoRepository, never()).findByLojaIdOrderByDataCriacaoDescComRelacionamentos(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("Usuario nao autenticado nao pode listar pedidos por loja")
    void usuarioNaoAutenticadoNaoPodeListarPedidosPorLoja() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> pedidoService.listarPorLoja(null, PageRequest.of(0, 10)))
                .isInstanceOf(AccessDeniedException.class);
        verify(pedidoRepository, never()).findAllWithRelacionamentos(any(Pageable.class));
        verify(pedidoRepository, never()).findByLojaIdOrderByDataCriacaoDescComRelacionamentos(anyLong(), any(Pageable.class));
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

        Page<PedidoRespostaDTO> resultado = pedidoService.listarMeusPedidos(pageable);

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

        PedidoRespostaDTO resultado = pedidoService.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(pedidoRepository).findByIdWithRelacionamentos(1L);
    }

    @Test
    @DisplayName("GERENTE deve buscar pedido da sua loja")
    void gerenteDeveBuscarPedidoDaSuaLoja() {
        autenticarComo(2L, "gerente@teste.com", PerfilUsuario.GERENTE, 3L);

        Pedido pedido = pedidoExemplo(1L, lojaExemplo(3L), clienteExemplo(10L));
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        PedidoRespostaDTO resultado = pedidoService.buscarPorId(1L);

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

        PedidoRespostaDTO resultado = pedidoService.buscarPorId(1L);

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
    }
}

