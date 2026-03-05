package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.ItemPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    Page<ItemPedido> findByPedidoId(Long pedidoId, Pageable pageable);

    Page<ItemPedido> findByProdutoId(Long produtoId, Pageable pageable);

    List<ItemPedido> findByPedidoId(Long pedidoId);

    long countByPedidoId(Long pedidoId);

    long countByProdutoId(Long produtoId);
}
