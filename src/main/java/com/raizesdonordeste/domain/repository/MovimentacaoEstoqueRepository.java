package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.estoque.loja.id = :lojaId ORDER BY m.dataCriacao DESC")
    Page<MovimentacaoEstoque> findByLojaId(@Param("lojaId") Long lojaId, Pageable pageable);

    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.estoque.loja.id = :lojaId AND m.estoque.produto.id = :produtoId ORDER BY m.dataCriacao DESC")
    Page<MovimentacaoEstoque> findByLojaIdAndProdutoId(@Param("lojaId") Long lojaId, @Param("produtoId") Long produtoId, Pageable pageable);

    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.estoque.id = :estoqueId ORDER BY m.dataCriacao DESC")
    Page<MovimentacaoEstoque> findByEstoqueId(@Param("estoqueId") Long estoqueId, Pageable pageable);

    @Query("SELECT m FROM MovimentacaoEstoque m WHERE m.estoque.loja.id = :lojaId AND m.dataCriacao BETWEEN :dataInicio AND :dataFim ORDER BY m.dataCriacao DESC")
    List<MovimentacaoEstoque> findByLojaIdAndDataCriacaoBetween(
            @Param("lojaId") Long lojaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim
    );

    @Query("SELECT COUNT(m) FROM MovimentacaoEstoque m WHERE m.estoque.produto.id = :produtoId AND m.estoque.loja.id = :lojaId")
    long countByProdutoIdAndLojaId(@Param("produtoId") Long produtoId, @Param("lojaId") Long lojaId);
}

