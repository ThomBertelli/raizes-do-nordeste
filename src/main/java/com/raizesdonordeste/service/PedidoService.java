package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pedido.PedidoRequestDTO;
import com.raizesdonordeste.api.dto.pedido.PedidoResponseDTO;
import com.raizesdonordeste.api.dto.pedido.PedidoStatusUpdateDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.enums.TipoTransacaoFidelidade;
import com.raizesdonordeste.domain.model.ItemPedido;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.model.SaldoFidelidade;
import com.raizesdonordeste.domain.model.TransacaoFidelidade;
import com.raizesdonordeste.domain.repository.PedidoRepository;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.domain.repository.ProdutoRepository;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import com.raizesdonordeste.domain.repository.SaldoFidelidadeRepository;
import com.raizesdonordeste.domain.repository.TransacaoFidelidadeRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.exception.RegraNegocioException;
import com.raizesdonordeste.infra.request.IdempotentResponse;
import com.raizesdonordeste.infra.request.RequestDeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import jakarta.persistence.OptimisticLockException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;

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
    private final RequestDeduplicationService requestDeduplicationService;
    private final TransacaoFidelidadeRepository transacaoFidelidadeRepository;
    private final SaldoFidelidadeRepository saldoFidelidadeRepository;
    private final ConfiguracaoFidelidadeService configuracaoFidelidadeService;

    private static final BigDecimal MOEDA_MINIMA = new BigDecimal("1.00");
    private static final int MAX_TENTATIVAS_RETRY = 3;

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
        if (principal.getPerfil() != PerfilUsuario.CLIENTE
                && principal.getPerfil() != PerfilUsuario.FUNCIONARIO
                && principal.getPerfil() != PerfilUsuario.GERENTE) {
            throw new AccessDeniedException("Perfil não autorizado para criar pedido");
        }

        return criarPedido(request, principal);
    }

    @Transactional
    public IdempotentResponse<PedidoResponseDTO> criarComIdempotencia(PedidoRequestDTO request, String idempotencyKey) {
        validarRequestCriacao(request);

        UsuarioAutenticado principal = securityContextService.getRequiredPrincipal();
        if (principal.getPerfil() != PerfilUsuario.CLIENTE
                && principal.getPerfil() != PerfilUsuario.FUNCIONARIO
                && principal.getPerfil() != PerfilUsuario.GERENTE) {
            throw new AccessDeniedException("Perfil não autorizado para criar pedido");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return new IdempotentResponse<>(criarPedido(request, principal), HttpStatus.CREATED.value());
        }

        String requestHash = calcularHashRequest(request);

        return requestDeduplicationService.execute(
                idempotencyKey,
                principal.getId(),
                requestHash,
                PedidoResponseDTO.class,
                () -> criarPedido(request, principal),
                HttpStatus.CREATED.value()
        );
    }

    private PedidoResponseDTO criarPedido(PedidoRequestDTO request, UsuarioAutenticado principal) {
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
                .descontoFidelidade(BigDecimal.ZERO)
                .itens(new ArrayList<>())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (var item : request.getItens()) {
            Produto produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));

            Estoque estoque = estoqueRepository.findByLojaIdAndProdutoIdWithLock(loja.getId(), produto.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque não encontrado para loja e produto informados"));

            if (estoque.getQuantidade() < item.getQuantidade()) {
                throw new RegraNegocioException("Estoque insuficiente para o produto informado");
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

        BigDecimal descontoFidelidade = aplicarDescontoFidelidade(request, cliente, total);
        BigDecimal totalFinal = total.subtract(descontoFidelidade);

        pedido.setDescontoFidelidade(descontoFidelidade);
        pedido.setValorTotal(totalFinal);

        Pedido salvo = pedidoRepository.save(pedido);
        registrarResgateFidelidadeObrigatorio(salvo, descontoFidelidade);
        log.info("Pedido criado: pedidoId={}, lojaId={}, valorTotal={}, actorId={}, actorPerfil={}",
                salvo.getId(),
                salvo.getLoja().getId(),
                salvo.getValorTotal(),
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());
        return toDTO(salvo);
    }

    @Transactional
    public PedidoResponseDTO atualizarStatusOperacaoLoja(Long pedidoId, PedidoStatusUpdateDTO request) {
        if (request == null || request.getStatusPedido() == null || request.getOrigem() == null) {
            throw new RegraNegocioException("statusPedido e origem são obrigatórios");
        }
        if (request.getOrigem() != PedidoStatusUpdateDTO.OrigemStatusPedido.OPERACAO_LOJA) {
            throw new RegraNegocioException("Origem inválida para atualização manual do status");
        }

        UsuarioAutenticado principal = securityContextService.getRequiredPrincipal();
        if (principal.getPerfil() != PerfilUsuario.FUNCIONARIO && principal.getPerfil() != PerfilUsuario.GERENTE) {
            throw new AccessDeniedException("Perfil não autorizado para atualizar status do pedido");
        }

        Pedido pedido = buscarEntidade(pedidoId);
        if (principal.getLojaId() == null || !principal.getLojaId().equals(pedido.getLoja().getId())) {
            throw new AccessDeniedException("Acesso negado: pedido de outra loja");
        }

        StatusPedido statusAtual = pedido.getStatusPedido();
        StatusPedido statusNovo = request.getStatusPedido();
        validarTransicaoOperacaoLoja(statusAtual, statusNovo);

        pedido.setStatusPedido(statusNovo);
        Pedido salvo = pedidoRepository.save(pedido);
        if (statusNovo == StatusPedido.ENTREGUE) {
            creditarFidelidadeSeElegivel(salvo);
        }
        log.info("Status do pedido atualizado: pedidoId={}, lojaId={}, statusAnterior={}, statusNovo={}, actorId={}, actorPerfil={}",
                salvo.getId(),
                salvo.getLoja().getId(),
                statusAtual,
                statusNovo,
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
            throw new RegraNegocioException("Dados do pedido são obrigatórios");
        }
        if (request.getCanalPedido() == null) {
            throw new RegraNegocioException("canalPedido é obrigatório");
        }
        if (request.getLojaId() == null) {
            throw new RegraNegocioException("lojaId é obrigatório");
        }
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new RegraNegocioException("itens são obrigatórios");
        }
    }

    private void validarTransicaoOperacaoLoja(StatusPedido statusAtual, StatusPedido statusNovo) {
        if (statusAtual == StatusPedido.CONFIRMADO && statusNovo == StatusPedido.PREPARO) {
            return;
        }
        if (statusAtual == StatusPedido.PREPARO && statusNovo == StatusPedido.PRONTO) {
            return;
        }
        if (statusAtual == StatusPedido.PRONTO && statusNovo == StatusPedido.ENTREGUE) {
            return;
        }
        throw new RegraNegocioException("Transição de status não permitida: " + statusAtual + " -> " + statusNovo);
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
                .descontoFidelidade(pedido.getDescontoFidelidade())
                .dataCriacao(pedido.getDataCriacao())
                .dataAtualizacao(pedido.getDataAtualizacao())
                .build();
    }

    private BigDecimal aplicarDescontoFidelidade(PedidoRequestDTO request, Usuario cliente, BigDecimal total) {
        if (request.getMoedasFidelidade() == null) {
            return BigDecimal.ZERO;
        }
        if (!cliente.isConsentimentoProgramaFidelidade()) {
            throw new RegraNegocioException("Cliente não aderiu ao programa de fidelidade");
        }
        BigDecimal moedas = request.getMoedasFidelidade().setScale(2, RoundingMode.DOWN);
        if (moedas.compareTo(MOEDA_MINIMA) < 0) {
            throw new RegraNegocioException("Moedas de fidelidade devem ser a partir de 1.00");
        }
        if (moedas.compareTo(total) > 0) {
            throw new RegraNegocioException("Desconto de fidelidade não pode exceder o valor do pedido");
        }
        BigDecimal saldo = obterSaldoMoedas(cliente.getId());
        if (saldo.compareTo(moedas) < 0) {
            throw new RegraNegocioException("Saldo de fidelidade insuficiente");
        }
        return moedas;
    }

    private void registrarResgateFidelidadeObrigatorio(Pedido pedido, BigDecimal moedas) {
        if (moedas == null || moedas.compareTo(BigDecimal.ZERO) <= 0 || pedido.getId() == null) {
            return;
        }
        executarComRetry(() -> {
            if (transacaoFidelidadeRepository.existsByPedidoIdAndTipo(pedido.getId(), TipoTransacaoFidelidade.RESGATE)) {
                return;
            }
            SaldoFidelidade saldo = carregarSaldoParaAtualizacao(pedido.getCliente());
            if (saldo.getMoedas().compareTo(moedas) < 0) {
                throw new RegraNegocioException("Saldo de fidelidade insuficiente");
            }
            TransacaoFidelidade transacao = TransacaoFidelidade.builder()
                    .usuario(pedido.getCliente())
                    .pedido(pedido)
                    .tipo(TipoTransacaoFidelidade.RESGATE)
                    .moedas(moedas)
                    .build();
            transacaoFidelidadeRepository.save(transacao);
            saldo.setMoedas(saldo.getMoedas().subtract(moedas));
            saldoFidelidadeRepository.save(saldo);
        });
    }

    private void creditarFidelidadeSeElegivel(Pedido pedido) {
        Usuario cliente = pedido.getCliente();
        if (cliente == null || !cliente.isConsentimentoProgramaFidelidade()) {
            return;
        }
        if (pedido.getId() != null && transacaoFidelidadeRepository
                .existsByPedidoIdAndTipo(pedido.getId(), TipoTransacaoFidelidade.GANHO)) {
            return;
        }
        BigDecimal moedas = calcularMoedasPorValorPago(pedido.getValorTotal());
        if (moedas.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        executarComRetry(() -> {
            if (pedido.getId() != null && transacaoFidelidadeRepository
                    .existsByPedidoIdAndTipo(pedido.getId(), TipoTransacaoFidelidade.GANHO)) {
                return;
            }
            TransacaoFidelidade transacao = TransacaoFidelidade.builder()
                    .usuario(cliente)
                    .pedido(pedido)
                    .tipo(TipoTransacaoFidelidade.GANHO)
                    .moedas(moedas)
                    .build();
            transacaoFidelidadeRepository.save(transacao);
            SaldoFidelidade saldo = carregarSaldoParaAtualizacao(cliente);
            saldo.setMoedas(saldo.getMoedas().add(moedas));
            saldoFidelidadeRepository.save(saldo);
        });
    }

    private BigDecimal obterSaldoMoedas(Long usuarioId) {
        return saldoFidelidadeRepository.findByUsuarioId(usuarioId)
                .map(SaldoFidelidade::getMoedas)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.DOWN);
    }

    private SaldoFidelidade carregarSaldoParaAtualizacao(Usuario cliente) {
        return saldoFidelidadeRepository.findByUsuarioId(cliente.getId())
                .orElseGet(() -> criarSaldoInicial(cliente));
    }

    private SaldoFidelidade criarSaldoInicial(Usuario cliente) {
        SaldoFidelidade novoSaldo = SaldoFidelidade.builder()
                .usuario(cliente)
                .moedas(BigDecimal.ZERO.setScale(2, RoundingMode.DOWN))
                .build();
        try {
            return saldoFidelidadeRepository.save(novoSaldo);
        } catch (DataIntegrityViolationException ex) {
            return saldoFidelidadeRepository.findByUsuarioId(cliente.getId())
                    .orElseThrow(() -> new RegraNegocioException("Saldo de fidelidade indisponível"));
        }
    }

    private void executarComRetry(Runnable acao) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS_RETRY; tentativa++) {
            try {
                acao.run();
                return;
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException ex) {
                if (tentativa == MAX_TENTATIVAS_RETRY) {
                    throw ex;
                }
            }
        }
    }

    private BigDecimal calcularMoedasPorValorPago(BigDecimal valorPago) {
        BigDecimal taxa = configuracaoFidelidadeService.obterTaxaConversao();
        return valorPago.multiply(taxa).setScale(2, RoundingMode.DOWN);
    }

    private String calcularHashRequest(PedidoRequestDTO request) {
        StringBuilder payload = new StringBuilder();
        payload.append(request.getLojaId()).append('|')
                .append(request.getCanalPedido());

        if (request.getMoedasFidelidade() != null) {
            payload.append('|').append(request.getMoedasFidelidade());
        }

        var itensOrdenados = new ArrayList<>(request.getItens());
        itensOrdenados.sort((a, b) -> {
            int compare = a.getProdutoId().compareTo(b.getProdutoId());
            if (compare != 0) {
                return compare;
            }
            return Integer.compare(a.getQuantidade(), b.getQuantidade());
        });

        for (var item : itensOrdenados) {
            payload.append('|')
                    .append(item.getProdutoId())
                    .append(':')
                    .append(item.getQuantidade());
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", ex);
        }
    }
}
