package com.raizesdonordeste.infra.request;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestDeduplicationServiceTest {

    @Mock
    private RequestDeduplicationRepository repository;

    @Mock
    private JsonMapper objectMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private RequestDeduplicationService service;

    @Test
    @DisplayName("Deve limpar o contexto de persistencia quando houver conflito ao criar registro idempotente")
    void deveLimparContextoQuandoHouverConflitoAoCriarRegistroIdempotente() {
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        when(repository.saveAndFlush(any(RequestExecutionRecord.class)))
                .thenThrow(new DataIntegrityViolationException("uk_request_execution_key_user"));
        when(repository.findByIdempotencyKeyAndUsuarioId("idem-1", 10L))
                .thenReturn(java.util.Optional.of(RequestExecutionRecord.builder()
                        .id(1L)
                        .idempotencyKey("idem-1")
                        .usuarioId(10L)
                        .requestHash("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                        .responseBody("{\"ok\":true}")
                        .statusCode(200)
                        .status(ExecutionStatus.SUCCESS)
                        .build()));
        when(objectMapper.readValue("{\"ok\":true}", DummyResponse.class))
                .thenReturn(new DummyResponse(true));

        IdempotentResponse<DummyResponse> response = service.execute(
                "idem-1",
                10L,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                DummyResponse.class,
                () -> new DummyResponse(false),
                200
        );

        assertThat(response.body().ok()).isTrue();
        verify(entityManager).clear();
    }

    private record DummyResponse(boolean ok) {
    }
}
