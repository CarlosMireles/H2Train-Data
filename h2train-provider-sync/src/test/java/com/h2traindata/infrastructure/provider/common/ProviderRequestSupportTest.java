package com.h2traindata.infrastructure.provider.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.h2traindata.application.exception.ProviderAuthorizationException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class ProviderRequestSupportTest {

    @Test
    void translatesInvalidGrantToSanitizedProviderAuthorizationException() {
        HttpClientErrorException exception = HttpClientErrorException.BadRequest.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                """
                        {"errors":[{"errorType":"invalid_grant","message":"Refresh token invalid: secret-token"}]}
                        """.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        ProviderAuthorizationException thrown = assertThrows(ProviderAuthorizationException.class, () ->
                ProviderRequestSupport.execute("fitbit", "refresh an access token", () -> {
                    throw exception;
                }));

        assertEquals("fitbit", thrown.providerId());
        assertEquals("refresh an access token", thrown.operation());
        assertEquals("Provider authorization is no longer valid; reconnect the provider account", thrown.getMessage());
    }
}
