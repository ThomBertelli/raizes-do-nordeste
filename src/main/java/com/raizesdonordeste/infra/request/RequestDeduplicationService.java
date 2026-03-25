package com.raizesdonordeste.infra.request;

import tools.jackson.databind.json.JsonMapper;
import com.raizesdonordeste.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RequestDeduplicationService {

    private final RequestDeduplicationRepository repository;
    private final JsonMapper objectMapper;

    @Transactional
    public <T> IdempotentResponse<T> execute(String idempotencyKey,
                                            Long usuarioId,
                                            String requestHash,
                                            Class<T> responseType,
                                            Supplier<T> supplier,
                                            int statusCode) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return new IdempotentResponse<>(supplier.get(), statusCode);
        }

        RequestExecutionRecord created = criarRegistroProcessando(idempotencyKey, usuarioId, requestHash);
        if (created == null) {
            RequestExecutionRecord existing = buscarRegistro(idempotencyKey, usuarioId);
            if (existing == null) {
                throw new RegraNegocioException("Falha ao recuperar requisição idempotente existente");
            }
            validarHashCompatavel(existing, requestHash);
            return responderExistente(existing, responseType);
        }

        try {
            T response = supplier.get();
            String responseBody = serializar(response);
            created.setResponseBody(responseBody);
            created.setStatusCode(statusCode);
            created.setStatus(ExecutionStatus.SUCCESS);
            repository.save(created);
            return new IdempotentResponse<>(response, statusCode);
        } catch (RuntimeException ex) {
            created.setResponseBody(ex.getMessage() != null ? ex.getMessage() : "Falha inesperada");
            created.setStatusCode(500);
            created.setStatus(ExecutionStatus.FAILED);
            repository.save(created);
            throw ex;
        }
    }

    private RequestExecutionRecord criarRegistroProcessando(String idempotencyKey,
                                                            Long usuarioId,
                                                            String requestHash) {
        if (requestHash == null || requestHash.isBlank()) {
            throw new RegraNegocioException("requestHash é obrigatório quando Idempotency-Key é informado");
        }

        RequestExecutionRecord novo = RequestExecutionRecord.builder()
                .idempotencyKey(idempotencyKey)
                .usuarioId(usuarioId)
                .requestHash(requestHash)
                .responseBody("")
                .statusCode(0)
                .status(ExecutionStatus.PROCESSING)
                .build();

        try {
            return repository.saveAndFlush(novo);
        } catch (DataIntegrityViolationException ex) {
            return null;
        }
    }

    private RequestExecutionRecord buscarRegistro(String idempotencyKey, Long usuarioId) {
        Optional<RequestExecutionRecord> existing = repository
                .findByIdempotencyKeyAndUsuarioId(idempotencyKey, usuarioId);
        return existing.orElse(null);
    }

    private void validarHashCompatavel(RequestExecutionRecord existing, String requestHash) {
        if (!requestHash.equals(existing.getRequestHash())) {
            throw new RegraNegocioException("Idempotency-Key já utilizado para uma requisição diferente");
        }
    }

    private <T> IdempotentResponse<T> responderExistente(RequestExecutionRecord existing, Class<T> responseType) {
        if (existing.getStatus() == ExecutionStatus.PROCESSING) {
            throw new RegraNegocioException("Requisição idempotente em processamento");
        }
        if (existing.getStatus() == ExecutionStatus.FAILED) {
            throw new RegraNegocioException("Requisição idempotente falhou anteriormente: " + existing.getResponseBody());
        }
        T body = deserializar(existing.getResponseBody(), responseType);
        return new IdempotentResponse<>(body, existing.getStatusCode());
    }

    private String serializar(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar resposta para idempotencia", ex);
        }
    }

    private <T> T deserializar(String payload, Class<T> responseType) {
        try {
            return objectMapper.readValue(payload, responseType);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao desserializar resposta de idempotencia", ex);
        }
    }
}
