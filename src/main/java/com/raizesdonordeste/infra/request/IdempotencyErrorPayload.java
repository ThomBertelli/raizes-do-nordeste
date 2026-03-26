package com.raizesdonordeste.infra.request;

import java.time.LocalDateTime;

public record IdempotencyErrorPayload(String errorType, String message, LocalDateTime timestamp) {
}

