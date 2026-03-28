package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.produto.ProdutoUpdateDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoCreateDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoDisponivelLojaResponseDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoResponseDTO;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.domain.repository.ProdutoRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final LojaRepository lojaRepository;
    private final SecurityContextService securityContextService;

    @Transactional
    public ProdutoResponseDTO criar(ProdutoCreateDTO dto) {
        if (produtoRepository.existsByNome(dto.getNome())) {
            throw new RegraNegocioException("Nome de produto já cadastrado");
        }

        Produto produto = Produto.builder()
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .preco(dto.getPreco())
                .ativo(true)
                .build();

        Produto salvo = produtoRepository.save(produto);
        log.info("Produto criado: produtoId={}, ativo={}, actorId={}, actorPerfil={}",
                salvo.getId(),
                salvo.isAtivo(),
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());
        return converterParaDTO(salvo);
    }

    @Transactional
    public ProdutoResponseDTO atualizar(Long id, ProdutoUpdateDTO dados) {
        Produto produto = buscarEntidade(id);

        if (dados.getNome() != null && !dados.getNome().equals(produto.getNome())) {
            if (produtoRepository.existsByNome(dados.getNome())) {
                throw new RegraNegocioException("Nome de produto já cadastrado");
            }
            produto.setNome(dados.getNome());
        }

        if (dados.getDescricao() != null) {
            produto.setDescricao(dados.getDescricao());
        }

        if (dados.getPreco() != null) {
            produto.setPreco(dados.getPreco());
        }

        Produto atualizado = produtoRepository.save(produto);
        log.info("Produto atualizado: produtoId={}, ativo={}, actorId={}, actorPerfil={}",
                atualizado.getId(),
                atualizado.isAtivo(),
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());
        return converterParaDTO(atualizado);
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Long id) {
        return converterParaDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> listarTodos(Pageable pageable) {
        return produtoRepository.findAll(pageable).map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> buscarAtivos(Pageable pageable) {
        return produtoRepository.findByAtivo(true, pageable).map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> buscarPorNome(String nome, Pageable pageable) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome, pageable).map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> buscarPorDescricao(String descricao, Pageable pageable) {
        return produtoRepository.findByDescricaoContainingIgnoreCase(descricao, pageable).map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> buscarPorFaixaDePreco(BigDecimal precoMin, BigDecimal precoMax, Pageable pageable) {
        return produtoRepository.findByPrecoBetween(precoMin, precoMax, pageable).map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoDisponivelLojaResponseDTO> listarDisponiveisPorLoja(Long lojaId, String nome, Pageable pageable) {
        Loja loja = lojaRepository.findById(lojaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja não encontrada"));

        if (!loja.isAtiva()) {
            throw new RegraNegocioException("Loja informada não está ativa para pedidos");
        }

        String nomeNormalizado = nome == null || nome.isBlank() ? null : nome.trim();

        if (nomeNormalizado == null) {
            return estoqueRepository.findProdutosDisponiveisParaVenda(lojaId, pageable)
                    .map(this::converterParaDisponivelLojaDTO);
        }

        return estoqueRepository.findProdutosDisponiveisParaVendaPorNome(lojaId, nomeNormalizado, pageable)
                .map(this::converterParaDisponivelLojaDTO);
    }

    @Transactional
    public void ativar(Long id) {
        Produto produto = buscarEntidade(id);
        produto.setAtivo(true);
        produtoRepository.save(produto);
        log.info("Produto ativado: produtoId={}, actorId={}, actorPerfil={}",
                id,
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());
    }

    @Transactional
    public void desativar(Long id) {
        Produto produto = buscarEntidade(id);
        produto.setAtivo(false);
        produtoRepository.save(produto);
        log.info("Produto desativado: produtoId={}, actorId={}, actorPerfil={}",
                id,
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());
    }

    @Transactional
    public void deletar(Long id) {
        if (!produtoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Produto não encontrado");
        }
        produtoRepository.deleteById(id);
        log.info("Produto deletado: produtoId={}, actorId={}, actorPerfil={}",
                id,
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());
    }

    private Produto buscarEntidade(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
    }

    private ProdutoResponseDTO converterParaDTO(Produto produto) {
        return ProdutoResponseDTO.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .preco(produto.getPreco())
                .ativo(produto.isAtivo())
                .dataCriacao(produto.getDataCriacao())
                .dataAtualizacao(produto.getDataAtualizacao())
                .build();
    }

    private ProdutoDisponivelLojaResponseDTO converterParaDisponivelLojaDTO(Estoque estoque) {
        Produto produto = estoque.getProduto();

        return ProdutoDisponivelLojaResponseDTO.builder()
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .preco(produto.getPreco())
                .build();
    }
}
