package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.enums.FormaPagamento;
import com.raizesdonordeste.domain.enums.StatusPagamento;
import com.raizesdonordeste.domain.model.Pagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByPedidoId(Long pedidoId);

    boolean existsByPedidoId(Long pedidoId);

    Page<Pagamento> findByStatusPagamento(StatusPagamento statusPagamento, Pageable pageable);

    Page<Pagamento> findByFormaPagamento(FormaPagamento formaPagamento, Pageable pageable);

    Page<Pagamento> findByCodigoTransacao(String codigoTransacao, Pageable pageable);

    long countByStatusPagamento(StatusPagamento statusPagamento);

    long countByFormaPagamento(FormaPagamento formaPagamento);
}

