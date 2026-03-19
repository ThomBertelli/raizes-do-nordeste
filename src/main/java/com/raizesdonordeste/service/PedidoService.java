package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pedido.PedidoRequestDTO;
import com.raizesdonordeste.api.dto.pedido.PedidoResponseDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.enums.CanalPedido;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoAuthorization pedidoAuthorization;
    private final LojaRepository lojaRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityContextService securityContextService;

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarPorLoja(Long lojaId, CanalPedido canalPedido, StatusPedido statusPedido, Pageable pageable) {
        UsuarioAutenticado principal = securityContextService.getRequiredPrincipal();
        Long lojaAutorizada = pedidoAuthorization.podeListarPedidos(principal, lojaId);
        log.info("Pedidos listados: lojaSolicitada={}, lojaAutorizada={}, canalPedido={}, statusPedido={}, actorId={}, actorPerfil={}",
                lojaId,
                lojaAutorizada,
                canalPedido,
                statusPedido,
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());

        Page<Pedido> paginaPedidos;
        if (lojaAutorizada == null) {
            if (canalPedido != null && statusPedido != null) {
                paginaPedidos = pedidoRepository
                        .findByCanalPedidoAndStatusPedidoOrderByDataCriacaoDescComRelacionamentos(canalPedido, statusPedido, pageable);
            } else if (canalPedido != null) {
                paginaPedidos = pedidoRepository
                        .findByCanalPedidoOrderByDataCriacaoDescComRelacionamentos(canalPedido, pageable);
            } else if (statusPedido != null) {
                paginaPedidos = pedidoRepository
                        .findByStatusPedidoOrderByDataCriacaoDescComRelacionamentos(statusPedido, pageable);
            } else {
                paginaPedidos = pedidoRepository.findAllWithRelacionamentos(pageable);
            }
        } else {
            if (canalPedido != null && statusPedido != null) {
                paginaPedidos = pedidoRepository
                        .findByLojaIdAndCanalPedidoAndStatusPedidoOrderByDataCriacaoDescComRelacionamentos(lojaAutorizada, canalPedido, statusPedido, pageable);
            } else if (canalPedido != null) {
                paginaPedidos = pedidoRepository
                        .findByLojaIdAndCanalPedidoOrderByDataCriacaoDescComRelacionamentos(lojaAutorizada, canalPedido, pageable);
            } else if (statusPedido != null) {
                paginaPedidos = pedidoRepository
                        .findByLojaIdAndStatusPedidoOrderByDataCriacaoDescComRelacionamentos(lojaAutorizada, statusPedido, pageable);
            } else {
                paginaPedidos = pedidoRepository.findByLojaIdOrderByDataCriacaoDescComRelacionamentos(lojaAutorizada, pageable);
            }
        }

        return paginaPedidos.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponseDTO> listarMeusPedidos(Pageable pageable) {
        UsuarioAutenticado principal = securityContextService.getRequiredPrincipal();

        pedidoAuthorization.exigirCliente(principal);
        log.info("Pedidos do cliente listados: actorId={}, actorPerfil={}",
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());

        return pedidoRepository
                .findByClienteIdOrderByDataCriacaoDescComRelacionamentos(principal.getId(), pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(Long id) {
        UsuarioAutenticado principal = securityContextService.getRequiredPrincipal();
        Pedido pedido = buscarEntidade(id);

        pedidoAuthorization.podeVisualizarPedido(principal, pedido);
        log.info("Pedido visualizado: pedidoId={}, lojaId={}, actorId={}, actorPerfil={}",
                pedido.getId(),
                pedido.getLoja().getId(),
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());

        return toDTO(pedido);
    }

    @Transactional
    public PedidoResponseDTO criar(PedidoRequestDTO request) {
        validarRequestCriacao(request);

        UsuarioAutenticado principal = securityContextService.getRequiredPrincipal();
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

        Pedido salvo = pedidoRepository.save(pedido);
        log.info("Pedido criado: pedidoId={}, lojaId={}, valorTotal={}, actorId={}, actorPerfil={}",
                salvo.getId(),
                salvo.getLoja().getId(),
                salvo.getValorTotal(),
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());
        return toDTO(salvo);
    }

    private Pedido buscarEntidade(Long id) {
        return pedidoRepository.findByIdWithRelacionamentos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    private void validarRequestCriacao(PedidoRequestDTO request) {
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


    private PedidoResponseDTO toDTO(Pedido pedido) {
        return PedidoResponseDTO.builder()
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
