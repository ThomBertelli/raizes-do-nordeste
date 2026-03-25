package com.raizesdonordeste.infra.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequestDeduplicationRepository extends JpaRepository<RequestExecutionRecord, Long> {

    Optional<RequestExecutionRecord> findByIdempotencyKeyAndUsuarioId(String idempotencyKey, Long usuarioId);

    boolean existsByIdempotencyKeyAndUsuarioId(String idempotencyKey, Long usuarioId);
}

