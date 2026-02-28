package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.Loja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LojaRepository extends JpaRepository<Loja, Long> {

    Optional<Loja> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    Page<Loja> findByAtiva(boolean ativa, Pageable pageable);

    Page<Loja> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Loja> findByEnderecoContainingIgnoreCase(String endereco, Pageable pageable);

    long countByAtivaTrue();

}

