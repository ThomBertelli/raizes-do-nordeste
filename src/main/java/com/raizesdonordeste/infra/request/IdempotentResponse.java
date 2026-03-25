package com.raizesdonordeste.infra.request;

public record IdempotentResponse<T>(T body, int statusCode) {
}

