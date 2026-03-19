package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pedido.CreatePedidoRequest;
import com.raizesdonordeste.api.dto.pedido.PedidoRespostaDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.ItemPedido;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.PedidoRepository;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.domain.repository.ProdutoRepository;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoAuthorization pedidoAuthorization;
    private final LojaRepository lojaRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final UsuarioRepository usuarioRepository;

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

    @Transactional
    public PedidoRespostaDTO criar(CreatePedidoRequest request) {
        validarRequestCriacao(request);

        UsuarioAutenticado principal = obterPrincipalAutenticado();
        pedidoAuthorization.exigirCliente(principal);

        Loja loja = lojaRepository.findById(request.getLojaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja não encontrada"));

        Usuario cliente = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        Pedido pedido = Pedido.builder()
                .loja(loja)
                .cliente(cliente)
                .canalPedido(request.getCanalPedido())
                .statusPedido(StatusPedido.CRIADO)
                .valorTotal(BigDecimal.ZERO)
                .itens(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (var item : request.getItens()) {
            Produto produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));

            Estoque estoque = estoqueRepository.findByLojaIdAndProdutoIdWithLock(loja.getId(), produto.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque não encontrado para loja e produto informados"));

            if (estoque.getQuantidade() < item.getQuantidade()) {
                throw new IllegalArgumentException("Estoque insuficiente para o produto informado");
            }

            estoque.setQuantidade(estoque.getQuantidade() - item.getQuantidade());
            estoqueRepository.save(estoque);

            ItemPedido itemPedido = ItemPedido.builder()
                    .pedido(pedido)
                    .produto(produto)
                    .quantidade(item.getQuantidade())
                    .precoUnitario(produto.getPreco())
                    .build();
            itemPedido.calcularSubtotal();

            pedido.getItens().add(itemPedido);
            total = total.add(itemPedido.getSubtotal());
        }

        pedido.setValorTotal(total);

        return toDTO(pedidoRepository.save(pedido));
    }

    private Pedido buscarEntidade(Long id) {
        return pedidoRepository.findByIdWithRelacionamentos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    private void validarRequestCriacao(CreatePedidoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dados do pedido são obrigatórios");
        }
        if (request.getCanalPedido() == null) {
            throw new IllegalArgumentException("canalPedido é obrigatório");
        }
        if (request.getLojaId() == null) {
            throw new IllegalArgumentException("lojaId é obrigatório");
        }
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new IllegalArgumentException("itens são obrigatórios");
        }
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
