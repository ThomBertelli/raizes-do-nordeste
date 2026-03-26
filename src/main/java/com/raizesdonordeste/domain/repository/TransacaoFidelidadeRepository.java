package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.enums.TipoTransacaoFidelidade;
import com.raizesdonordeste.domain.model.TransacaoFidelidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface TransacaoFidelidadeRepository extends JpaRepository<TransacaoFidelidade, Long> {

    boolean existsByPedidoIdAndTipo(Long pedidoId, TipoTransacaoFidelidade tipo);

    Page<TransacaoFidelidade> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId, Pageable pageable);

}

