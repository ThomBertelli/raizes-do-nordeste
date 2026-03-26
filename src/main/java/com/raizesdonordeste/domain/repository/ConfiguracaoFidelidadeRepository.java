package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.model.ConfiguracaoFidelidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracaoFidelidadeRepository extends JpaRepository<ConfiguracaoFidelidade, Long> {

	java.util.Optional<ConfiguracaoFidelidade> findFirstByOrderByIdAsc();
}


