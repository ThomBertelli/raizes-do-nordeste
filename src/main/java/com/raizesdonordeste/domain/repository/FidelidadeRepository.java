package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.Fidelidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FidelidadeRepository extends JpaRepository<Fidelidade, Long> {

    Optional<Fidelidade> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    Page<Fidelidade> findByPontosAtuaisGreaterThanEqual(int pontos, Pageable pageable);

    Page<Fidelidade> findByPontosTotaisAcumuladosGreaterThanEqual(int pontos, Pageable pageable);

    long countByPontosAtuaisGreaterThan(int pontos);
}

