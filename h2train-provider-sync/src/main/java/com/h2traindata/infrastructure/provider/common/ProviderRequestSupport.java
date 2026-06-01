package com.h2traindata.infrastructure.provider.common;

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
}
