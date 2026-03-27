package com.raizesdonordeste.infra.request;

import tools.jackson.databind.json.JsonMapper;
import com.raizesdonordeste.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RequestDeduplicationService {

    private static final int REQUEST_HASH_LENGTH = 64;
    private static final long PROCESSING_TIMEOUT_SECONDS = 120;
    private static final long FAILED_RETRY_SECONDS = 30;

    private final RequestDeduplicationRepository repository;
    private final JsonMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

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

        validarHashFormat(requestHash);

        RequestExecutionRecord created = criarRegistroProcessando(idempotencyKey, usuarioId, requestHash);
        if (created == null) {
            RequestExecutionRecord existing = buscarRegistro(idempotencyKey, usuarioId);
            if (existing == null) {
                throw new RegraNegocioException("Falha ao recuperar requisição idempotente existente");
            }
            validarHashCompatavel(existing, requestHash);
            IdempotentResponse<T> takeover = tentarTakeover(existing, idempotencyKey, usuarioId, requestHash, responseType, supplier, statusCode);
            if (takeover != null) {
                return takeover;
            }
            return responderExistente(existing, responseType);
        }

        try {
            T response = supplier.get();
            String responseBody = serializar(response);
            created.setResponseBody(responseBody);
            created.setStatusCode(statusCode);
            created.setStatus(ExecutionStatus.SUCCESS);
            return new IdempotentResponse<>(response, statusCode);
        } catch (RuntimeException ex) {
            created.setResponseBody(serializar(erroPayload(ex)));
            created.setStatusCode(resolverStatusCode(ex));
            created.setStatus(ExecutionStatus.FAILED);
            throw ex;
        }
    }

    private <T> IdempotentResponse<T> tentarTakeover(RequestExecutionRecord existing,
                                                     String idempotencyKey,
                                                     Long usuarioId,
                                                     String requestHash,
                                                     Class<T> responseType,
                                                     Supplier<T> supplier,
                                                     int statusCode) {
        if (existing.getStatus() == ExecutionStatus.PROCESSING && isProcessingExpired(existing)) {
            return executarTakeover(existing, idempotencyKey, usuarioId, requestHash, responseType, supplier, statusCode);
        }
        if (existing.getStatus() == ExecutionStatus.FAILED && isFailedRetryable(existing)) {
            return executarTakeover(existing, idempotencyKey, usuarioId, requestHash, responseType, supplier, statusCode);
        }
        return null;
    }

    private <T> IdempotentResponse<T> executarTakeover(RequestExecutionRecord existing,
                                                       String idempotencyKey,
                                                       Long usuarioId,
                                                       String requestHash,
                                                       Class<T> responseType,
                                                       Supplier<T> supplier,
                                                       int statusCode) {
        repository.delete(existing);
        repository.flush();

        RequestExecutionRecord created = criarRegistroProcessando(idempotencyKey, usuarioId, requestHash);
        if (created == null) {
            RequestExecutionRecord refreshed = buscarRegistro(idempotencyKey, usuarioId);
            if (refreshed == null) {
                throw new RegraNegocioException("Falha ao recuperar requisição idempotente existente");
            }
            return responderExistente(refreshed, responseType);
        }

        try {
            T response = supplier.get();
            created.setResponseBody(serializar(response));
            created.setStatusCode(statusCode);
            created.setStatus(ExecutionStatus.SUCCESS);
            return new IdempotentResponse<>(response, statusCode);
        } catch (RuntimeException ex) {
            created.setResponseBody(serializar(erroPayload(ex)));
            created.setStatusCode(resolverStatusCode(ex));
            created.setStatus(ExecutionStatus.FAILED);
            throw ex;
        }
    }

    private RequestExecutionRecord criarRegistroProcessando(String idempotencyKey,
                                                            Long usuarioId,
                                                            String requestHash) {
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
            entityManager.clear();
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

    private void validarHashFormat(String requestHash) {
        if (requestHash == null || requestHash.isBlank()) {
            throw new RegraNegocioException("requestHash é obrigatório quando Idempotency-Key é informado");
        }
        String normalized = requestHash.toLowerCase(Locale.ROOT);
        if (normalized.length() != REQUEST_HASH_LENGTH || !normalized.matches("[0-9a-f]+")) {
            throw new RegraNegocioException("requestHash inválido para Idempotency-Key");
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

    private boolean isProcessingExpired(RequestExecutionRecord existing) {
        LocalDateTime limite = LocalDateTime.now().minus(PROCESSING_TIMEOUT_SECONDS, ChronoUnit.SECONDS);
        return existing.getCreatedAt() != null && existing.getCreatedAt().isBefore(limite);
    }

    private boolean isFailedRetryable(RequestExecutionRecord existing) {
        LocalDateTime limite = LocalDateTime.now().minus(FAILED_RETRY_SECONDS, ChronoUnit.SECONDS);
        return existing.getUpdatedAt() != null && existing.getUpdatedAt().isBefore(limite);
    }

    private int resolverStatusCode(RuntimeException ex) {
        if (ex instanceof RegraNegocioException) {
            return HttpStatus.BAD_REQUEST.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private IdempotencyErrorPayload erroPayload(RuntimeException ex) {
        return new IdempotencyErrorPayload(
                ex.getClass().getSimpleName(),
                ex.getMessage() != null ? ex.getMessage() : "Falha inesperada",
                LocalDateTime.now()
        );
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
