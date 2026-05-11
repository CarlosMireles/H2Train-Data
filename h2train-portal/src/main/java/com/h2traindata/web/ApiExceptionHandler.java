package com.h2traindata.web;

import com.h2traindata.application.exception.ConnectionNotFoundException;
import com.h2traindata.application.exception.ProviderRateLimitException;
import com.h2traindata.application.exception.UnknownProviderException;
import com.h2traindata.application.exception.UnsupportedEventTypeException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownProviderException.class)
    public ResponseEntity<Map<String, String>> handleUnknownProvider(UnknownProviderException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ConnectionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleConnectionNotFound(ConnectionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedEventTypeException.class)
    public ResponseEntity<Map<String, String>> handleUnsupportedEventType(UnsupportedEventTypeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ProviderRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleProviderRateLimit(ProviderRateLimitException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage());
        if (exception.retryAfterSeconds() != null) {
            body.put("retryAfterSeconds", exception.retryAfterSeconds());
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(body);
    }
}
