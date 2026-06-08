package com.h2traindata.application.exception;

public class ProviderAuthorizationException extends RuntimeException {

    private final String providerId;
    private final String operation;

    public ProviderAuthorizationException(String providerId, String operation) {
        super("Provider authorization is no longer valid; reconnect the provider account");
        this.providerId = providerId;
        this.operation = operation;
    }

    public String providerId() {
        return providerId;
    }

    public String operation() {
        return operation;
    }
}
