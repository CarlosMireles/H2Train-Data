package com.h2traindata.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.h2traindata.application.exception.ProviderRateLimitException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsProviderRateLimitToHttp429() {
        ResponseEntity<Map<String, Object>> response = handler.handleProviderRateLimit(
                new ProviderRateLimitException("fitbit", "fetch Fitbit activity logs", 45L, null)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(45L, response.getBody().get("retryAfterSeconds"));
    }
}
