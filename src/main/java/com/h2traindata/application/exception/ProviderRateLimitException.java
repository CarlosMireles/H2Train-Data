package com.h2traindata.application.exception;

public class ProviderRateLimitException extends RuntimeException {

    private final String providerId;
    private final String operation;
    private final Long retryAfterSeconds;

    public ProviderRateLimitException(String providerId, String operation, Long retryAfterSeconds, Throwable cause) {
        super(buildMessage(providerId, operation, retryAfterSeconds), cause);
        this.providerId = providerId;
        this.operation = operation;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String providerId() {
        return providerId;
    }

    public String operation() {
        return operation;
    }

    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String buildMessage(String providerId, String operation, Long retryAfterSeconds) {
        String baseMessage = "Provider " + providerId + " rate limit reached while attempting to " + operation;
        if (retryAfterSeconds == null) {
            return baseMessage + ".";
        }
        return baseMessage + ". Retry after " + retryAfterSeconds + " seconds.";
    }
}
