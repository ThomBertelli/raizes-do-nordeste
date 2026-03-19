package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.produto.ProdutoUpdateDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoCreateDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoResponseDTO;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.repository.ProdutoRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    @Transactional
    public ProdutoResponseDTO criar(ProdutoCreateDTO dto) {
        if (produtoRepository.existsByNome(dto.getNome())) {
            throw new IllegalArgumentException("Nome de produto já cadastrado");
        }

        Produto produto = Produto.builder()
                .nome(dto.getNome())
                .descricao(dto.getDescricao())
                .preco(dto.getPreco())
                .ativo(true)
                .build();

        Produto salvo = produtoRepository.save(produto);
        return converterParaDTO(salvo);
    }

    @Transactional
    public ProdutoResponseDTO atualizar(Long id, ProdutoUpdateDTO dados) {
        Produto produto = buscarEntidade(id);

        if (dados.getNome() != null && !dados.getNome().equals(produto.getNome())) {
            if (produtoRepository.existsByNome(dados.getNome())) {
                throw new IllegalArgumentException("Nome de produto já cadastrado");
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

    @Transactional
    public void ativar(Long id) {
        Produto produto = buscarEntidade(id);
        produto.setAtivo(true);
        produtoRepository.save(produto);
    }

    @Transactional
    public void desativar(Long id) {
        Produto produto = buscarEntidade(id);
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    @Transactional
    public void deletar(Long id) {
        if (!produtoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Produto não encontrado");
        }
        produtoRepository.deleteById(id);
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
}

