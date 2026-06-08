package com.h2traindata.infrastructure.provider.common;

import com.h2traindata.application.exception.ProviderAuthorizationException;
import com.h2traindata.application.exception.ProviderRateLimitException;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

public final class ProviderRequestSupport {

    private ProviderRequestSupport() {
    }

    public static <T> T execute(String providerId, String operation, Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.TooManyRequests exception) {
            throw new ProviderRateLimitException(providerId, operation, retryAfterSeconds(exception.getResponseHeaders()), exception);
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ProviderAuthorizationException(providerId, operation);
        } catch (HttpClientErrorException.BadRequest exception) {
            if (isInvalidGrant(exception)) {
                throw new ProviderAuthorizationException(providerId, operation);
            }
            throw exception;
        }
    }

    public static <T> T getOrDefault(Supplier<T> request, T fallback) {
        try {
            T response = request.get();
            return response != null ? response : fallback;
        } catch (ProviderRateLimitException | RestClientException ignored) {
            return fallback;
        }
    }

    private static Long retryAfterSeconds(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter == null) {
            return null;
        }
        try {
            return Long.parseLong(retryAfter);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isInvalidGrant(HttpClientErrorException exception) {
        String body = exception.getResponseBodyAsString();
        return body != null && body.contains("invalid_grant");
    }
}
