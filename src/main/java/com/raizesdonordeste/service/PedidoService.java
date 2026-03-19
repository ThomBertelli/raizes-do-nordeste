package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pedido.PedidoRespostaDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.repository.PedidoRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoAuthorization pedidoAuthorization;

    @Transactional(readOnly = true)
    public Page<PedidoRespostaDTO> listarPorLoja(Long lojaId, Pageable pageable) {
        UsuarioAutenticado principal = obterPrincipalAutenticado();
        Long lojaAutorizada = pedidoAuthorization.podeListarPedidos(principal, lojaId);

        Page<Pedido> paginaPedidos;
        if (lojaAutorizada == null) {
            paginaPedidos = pedidoRepository.findAllWithRelacionamentos(pageable);
        } else {
            paginaPedidos = pedidoRepository.findByLojaIdOrderByDataCriacaoDescComRelacionamentos(lojaAutorizada, pageable);
        }

        return paginaPedidos.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PedidoRespostaDTO> listarMeusPedidos(Pageable pageable) {
        UsuarioAutenticado principal = obterPrincipalAutenticado();

        pedidoAuthorization.exigirCliente(principal);

        return pedidoRepository
                .findByClienteIdOrderByDataCriacaoDescComRelacionamentos(principal.getId(), pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public PedidoRespostaDTO buscarPorId(Long id) {
        UsuarioAutenticado principal = obterPrincipalAutenticado();
        Pedido pedido = buscarEntidade(id);

        pedidoAuthorization.podeVisualizarPedido(principal, pedido);

        return toDTO(pedido);
    }

    private Pedido buscarEntidade(Long id) {
        return pedidoRepository.findByIdWithRelacionamentos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    private UsuarioAutenticado obterPrincipalAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof UsuarioAutenticado usuarioAutenticado)) {
            throw new AccessDeniedException("Principal autenticado inválido");
        }

        return usuarioAutenticado;
    }

    private PedidoRespostaDTO toDTO(Pedido pedido) {
        return PedidoRespostaDTO.builder()
                .id(pedido.getId())
                .lojaId(pedido.getLoja().getId())
                .lojaNome(pedido.getLoja().getNome())
                .clienteId(pedido.getCliente().getId())
                .clienteNome(pedido.getCliente().getNome())
                .canalPedido(pedido.getCanalPedido())
                .statusPedido(pedido.getStatusPedido())
                .valorTotal(pedido.getValorTotal())
                .dataCriacao(pedido.getDataCriacao())
                .dataAtualizacao(pedido.getDataAtualizacao())
                .build();
    }
}
