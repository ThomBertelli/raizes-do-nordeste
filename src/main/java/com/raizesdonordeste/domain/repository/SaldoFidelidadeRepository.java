package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.SaldoFidelidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaldoFidelidadeRepository extends JpaRepository<SaldoFidelidade, Long> {

    Optional<SaldoFidelidade> findByUsuarioId(Long usuarioId);
}

