package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.estoque.EstoqueRequestDTO;
import com.raizesdonordeste.api.dto.estoque.MovimentacaoEstoqueResponseDTO;
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
import com.raizesdonordeste.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final LojaRepository lojaRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityContextService securityContextService;

    @Transactional
    public Estoque registrarEntrada(Long lojaId, Long produtoId, Integer quantidade, String motivo) {
        validarQuantidade(quantidade);
        Long lojaAutorizadaId = resolverLojaObrigatoriaParaMovimentacao(lojaId);

        Estoque estoque = estoqueRepository.findByLojaIdAndProdutoIdWithLock(lojaAutorizadaId, produtoId)
                .orElseGet(() -> buscarOuCriarEstoqueComRetry(lojaAutorizadaId, produtoId));

        estoque.setQuantidade(estoque.getQuantidade() + quantidade);
        Estoque estoqueAtualizado = estoqueRepository.save(estoque);

        registrarMovimentacao(estoqueAtualizado, TipoMovimentacaoEstoque.ENTRADA, quantidade, motivo);
        return estoqueAtualizado;
    }

    @Transactional
    public Estoque registrarSaida(Long lojaId, Long produtoId, Integer quantidade, String motivo) {
        validarQuantidade(quantidade);
        Long lojaAutorizadaId = resolverLojaObrigatoriaParaMovimentacao(lojaId);

        Estoque estoque = estoqueRepository.findByLojaIdAndProdutoIdWithLock(lojaAutorizadaId, produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque não encontrado para loja e produto informados"));

        if (quantidade > estoque.getQuantidade()) {
            throw new RegraNegocioException("Saldo insuficiente em estoque");
        }

        estoque.setQuantidade(estoque.getQuantidade() - quantidade);
        Estoque estoqueAtualizado = estoqueRepository.save(estoque);

        registrarMovimentacao(estoqueAtualizado, TipoMovimentacaoEstoque.SAIDA, quantidade, motivo);
        return estoqueAtualizado;
    }

    @Transactional(readOnly = true)
    public Page<Estoque> listarEstoquesPorLoja(Long lojaId, Pageable pageable) {
        Long lojaAutorizadaId = validarAcessoEstoque(lojaId);

        if (lojaAutorizadaId == null) {
            return estoqueRepository.findAll(pageable);
        }

        return estoqueRepository.findByLojaId(lojaAutorizadaId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> listarMovimentacoesPorLoja(Long lojaId, Long produtoId, Pageable pageable) {
        Long lojaAutorizadaId = validarAcessoEstoque(lojaId);

        if (lojaAutorizadaId == null) {
            if (produtoId != null) {
                return movimentacaoEstoqueRepository.findByProdutoId(produtoId, pageable);
            }
            return movimentacaoEstoqueRepository.findAll(pageable);
        }

        if (produtoId != null) {
            return movimentacaoEstoqueRepository.findByLojaIdAndProdutoId(lojaAutorizadaId, produtoId, pageable);
        }

        return movimentacaoEstoqueRepository.findByLojaId(lojaAutorizadaId, pageable);
    }

    public Long validarAcessoEstoque(Long lojaId) {
        UsuarioAutenticado usuarioAutenticado = obterUsuarioAutenticado();
        PerfilUsuario perfil = usuarioAutenticado.getPerfil();

        if (perfil == PerfilUsuario.GERENCIA_MATRIZ) {
            return lojaId;
        }

        if (perfil == PerfilUsuario.GERENTE) {
            if (usuarioAutenticado.getLojaId() == null) {
                throw new AccessDeniedException("Gerente sem loja vinculada não pode acessar estoque");
            }

            Long lojaDoGerenteId = usuarioAutenticado.getLojaId();

            if (lojaId != null && !lojaDoGerenteId.equals(lojaId)) {
                throw new AccessDeniedException("Gerente não pode acessar estoque de outra loja");
            }

            return lojaDoGerenteId;
        }

        throw new AccessDeniedException("Perfil sem permissão para acessar estoque");
    }

    private Long resolverLojaObrigatoriaParaMovimentacao(Long lojaId) {
        Long lojaAutorizadaId = validarAcessoEstoque(lojaId);
        if (lojaAutorizadaId == null) {
            throw new RegraNegocioException("lojaId é obrigatório para realizar movimentação de estoque");
        }
        return lojaAutorizadaId;
    }

    private Estoque criarEstoqueInicial(Long lojaId, Long produtoId) {
        Loja loja = lojaRepository.findById(lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja não encontrada"));

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));

        Estoque estoque = Estoque.builder()
                .loja(loja)
                .produto(produto)
                .quantidade(0)
                .build();

        return estoqueRepository.save(estoque);
    }

    private Estoque buscarOuCriarEstoqueComRetry(Long lojaId, Long produtoId) {
        try {
            return criarEstoqueInicial(lojaId, produtoId);
        } catch (DataIntegrityViolationException e) {
            return estoqueRepository.findByLojaIdAndProdutoIdWithLock(lojaId, produtoId)
                    .orElseThrow(() -> new IllegalStateException("Falha ao recuperar estoque após conflito de concorrência", e));
        }
    }

    private void registrarMovimentacao(Estoque estoque,
                                       TipoMovimentacaoEstoque tipo,
                                       Integer quantidade,
                                       String motivo) {
        UsuarioAutenticado principal = obterUsuarioAutenticado();
        Usuario usuario = usuarioRepository.getReferenceById(principal.getId());

        MovimentacaoEstoque movimentacao = MovimentacaoEstoque.builder()
                .estoque(estoque)
                .tipo(tipo)
                .quantidade(quantidade)
                .motivo(motivo)
                .usuario(usuario)
                .build();

        movimentacaoEstoqueRepository.save(movimentacao);
        log.info(
                "Movimentacao de estoque registrada: tipo={}, estoqueId={}, lojaId={}, produtoId={}, quantidade={}, usuarioId={}",
                tipo,
                estoque.getId(),
                estoque.getLoja().getId(),
                estoque.getProduto().getId(),
                quantidade,
                principal.getId()
        );
    }

    private void validarQuantidade(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new RegraNegocioException("Quantidade deve ser maior que zero");
        }
    }

    private UsuarioAutenticado obterUsuarioAutenticado() {
        return securityContextService.getRequiredPrincipal();
    }

    // Métodos para DTO

    @Transactional(readOnly = true)
    public Page<EstoqueRequestDTO> listarEstoquesPorLojaDTO(Long lojaId, Pageable pageable) {
        return listarEstoquesPorLoja(lojaId, pageable)
                .map(this::toEstoqueRespostaDTO);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoqueResponseDTO> listarMovimentacoesPorLojaDTO(Long lojaId, Long produtoId, Pageable pageable) {
        return listarMovimentacoesPorLoja(lojaId, produtoId, pageable)
                .map(this::toMovimentacaoRespostaDTO);
    }

    @Transactional
    public EstoqueRequestDTO registrarEntradaDTO(Long lojaId, Long produtoId, Integer quantidade, String motivo) {
        Estoque estoque = registrarEntrada(lojaId, produtoId, quantidade, motivo);
        return toEstoqueRespostaDTO(estoque);
    }

    @Transactional
    public EstoqueRequestDTO registrarSaidaDTO(Long lojaId, Long produtoId, Integer quantidade, String motivo) {
        Estoque estoque = registrarSaida(lojaId, produtoId, quantidade, motivo);
        return toEstoqueRespostaDTO(estoque);
    }

    private EstoqueRequestDTO toEstoqueRespostaDTO(Estoque estoque) {
        return EstoqueRequestDTO.builder()
                .id(estoque.getId())
                .lojaId(estoque.getLoja().getId())
                .lojaNome(estoque.getLoja().getNome())
                .produtoId(estoque.getProduto().getId())
                .produtoNome(estoque.getProduto().getNome())
                .quantidade(estoque.getQuantidade())
                .versao(estoque.getVersao())
                .dataCriacao(estoque.getDataCriacao())
                .dataAtualizacao(estoque.getDataAtualizacao())
                .build();
    }

    private MovimentacaoEstoqueResponseDTO toMovimentacaoRespostaDTO(MovimentacaoEstoque movimentacao) {
        return MovimentacaoEstoqueResponseDTO.builder()
                .id(movimentacao.getId())
                .estoqueId(movimentacao.getEstoque().getId())
                .lojaId(movimentacao.getEstoque().getLoja().getId())
                .produtoId(movimentacao.getEstoque().getProduto().getId())
                .tipo(movimentacao.getTipo())
                .quantidade(movimentacao.getQuantidade())
                .motivo(movimentacao.getMotivo())
                .usuarioId(movimentacao.getUsuario().getId())
                .usuarioNome(movimentacao.getUsuario().getNome())
                .dataCriacao(movimentacao.getDataCriacao())
                .build();
    }
}
