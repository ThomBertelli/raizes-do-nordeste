package com.raizesdonordeste.service;

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
    public Produto criar(Produto produto) {
        if (produtoRepository.existsByNome(produto.getNome())) {
            throw new IllegalArgumentException("Nome de produto já cadastrado");
        }

        produto.setId(null);
        produto.setAtivo(true);

        return produtoRepository.save(produto);
    }

    @Transactional
    public Produto atualizar(Long id, Produto dados) {
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

        return produtoRepository.save(produto);
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return buscarEntidade(id);
    }

    @Transactional(readOnly = true)
    public Page<Produto> listarTodos(Pageable pageable) {
        return produtoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Produto> buscarAtivos(Pageable pageable) {
        return produtoRepository.findByAtivo(true, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Produto> buscarPorNome(String nome, Pageable pageable) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Produto> buscarPorDescricao(String descricao, Pageable pageable) {
        return produtoRepository.findByDescricaoContainingIgnoreCase(descricao, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Produto> buscarPorFaixaDePreco(BigDecimal precoMin, BigDecimal precoMax, Pageable pageable) {
        return produtoRepository.findByPrecoBetween(precoMin, precoMax, pageable);
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
}

