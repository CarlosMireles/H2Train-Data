package com.h2traindata.web;

import com.h2traindata.application.exception.ConnectionNotFoundException;
import com.h2traindata.application.exception.AuthenticationRequiredException;
import com.h2traindata.application.exception.DuplicateUserAccountException;
import com.h2traindata.application.exception.EmailAlreadyInUseException;
import com.h2traindata.application.exception.EmailConfirmationMismatchException;
import com.h2traindata.application.exception.EmailUnchangedException;
import com.h2traindata.application.exception.ForbiddenAccountAccessException;
import com.h2traindata.application.exception.InvalidCredentialsException;
import com.h2traindata.application.exception.InvalidCurrentPasswordException;
import com.h2traindata.application.exception.PasswordConfirmationMismatchException;
import com.h2traindata.application.exception.PasswordUnchangedException;
import com.h2traindata.application.exception.ProviderAuthorizationException;
import com.h2traindata.application.exception.ProviderConnectionAlreadyLinkedException;
import com.h2traindata.application.exception.ProviderRateLimitException;
import com.h2traindata.application.exception.UnknownProviderException;
import com.h2traindata.application.exception.UnsupportedEventTypeException;
import com.h2traindata.application.exception.UserAccountNotFoundException;
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

    @ExceptionHandler(UserAccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserAccountNotFound(UserAccountNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateUserAccountException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUserAccount(DuplicateUserAccountException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyInUse(EmailAlreadyInUseException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ProviderConnectionAlreadyLinkedException.class)
    public ResponseEntity<Map<String, String>> handleProviderConnectionAlreadyLinked(
            ProviderConnectionAlreadyLinkedException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({AuthenticationRequiredException.class, InvalidCredentialsException.class})
    public ResponseEntity<Map<String, String>> handleAuthenticationRequired(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCurrentPassword(
            InvalidCurrentPasswordException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({
            EmailConfirmationMismatchException.class,
            EmailUnchangedException.class,
            PasswordConfirmationMismatchException.class,
            PasswordUnchangedException.class
    })
    public ResponseEntity<Map<String, String>> handleCredentialValidation(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ForbiddenAccountAccessException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenAccountAccess(ForbiddenAccountAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
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

    @ExceptionHandler(ProviderAuthorizationException.class)
    public ResponseEntity<Map<String, Object>> handleProviderAuthorization(ProviderAuthorizationException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage());
        body.put("provider", exception.providerId());
        body.put("operation", exception.operation());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body);
    }
}
