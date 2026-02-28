package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByNome(String nome);

    boolean existsByNome(String nome);

    Page<Produto> findByAtivo(boolean ativo, Pageable pageable);

    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Produto> findByDescricaoContainingIgnoreCase(String descricao, Pageable pageable);

    Page<Produto> findByPrecoBetween(BigDecimal precoMin, BigDecimal precoMax, Pageable pageable);


}

