package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findById(Long id);

    @Query(value = "SELECT p FROM Pedido p JOIN FETCH p.loja JOIN FETCH p.cliente",
            countQuery = "SELECT COUNT(p) FROM Pedido p")
    Page<Pedido> findAllWithRelacionamentos(Pageable pageable);

    @Query(value = "SELECT p FROM Pedido p JOIN FETCH p.loja JOIN FETCH p.cliente WHERE p.loja.id = :lojaId ORDER BY p.dataCriacao DESC",
            countQuery = "SELECT COUNT(p) FROM Pedido p WHERE p.loja.id = :lojaId")
    Page<Pedido> findByLojaIdOrderByDataCriacaoDescComRelacionamentos(@Param("lojaId") Long lojaId, Pageable pageable);

    @Query(value = "SELECT p FROM Pedido p JOIN FETCH p.loja JOIN FETCH p.cliente WHERE p.cliente.id = :clienteId ORDER BY p.dataCriacao DESC",
            countQuery = "SELECT COUNT(p) FROM Pedido p WHERE p.cliente.id = :clienteId")
    Page<Pedido> findByClienteIdOrderByDataCriacaoDescComRelacionamentos(@Param("clienteId") Long clienteId, Pageable pageable);

    @Query("SELECT p FROM Pedido p JOIN FETCH p.loja JOIN FETCH p.cliente WHERE p.id = :id")
    Optional<Pedido> findByIdWithRelacionamentos(@Param("id") Long id);

    Page<Pedido> findByLojaId(Long lojaId, Pageable pageable);

    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);

    Page<Pedido> findByLojaIdAndClienteId(Long lojaId, Long clienteId, Pageable pageable);

    Page<Pedido> findByLojaIdAndStatusPedido(Long lojaId, StatusPedido status, Pageable pageable);

    @Query("SELECT p FROM Pedido p WHERE p.loja.id = :lojaId ORDER BY p.dataCriacao DESC")
    Page<Pedido> findByLojaIdOrderByDataCriacaoDesc(@Param("lojaId") Long lojaId, Pageable pageable);

    @Query("SELECT p FROM Pedido p WHERE p.cliente.id = :clienteId ORDER BY p.dataCriacao DESC")
    Page<Pedido> findByClienteIdOrderByDataCriacaoDesc(@Param("clienteId") Long clienteId, Pageable pageable);
}

