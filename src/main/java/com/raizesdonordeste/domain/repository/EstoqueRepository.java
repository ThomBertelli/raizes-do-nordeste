package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByLojaIdAndProdutoId(Long lojaId, Long produtoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Estoque e WHERE e.loja.id = :lojaId AND e.produto.id = :produtoId")
    Optional<Estoque> findByLojaIdAndProdutoIdWithLock(@Param("lojaId") Long lojaId, @Param("produtoId") Long produtoId);

    Page<Estoque> findByLojaId(Long lojaId, Pageable pageable);

    Page<Estoque> findByProdutoId(Long produtoId, Pageable pageable);

    Page<Estoque> findByLojaIdAndQuantidadeLessThan(
            Long lojaId,
            Integer quantidade,
            Pageable pageable
    );
}
