package com.h2traindata.application.exception;

public class UnknownProviderException extends RuntimeException {

    public UnknownProviderException(String providerId) {
        super("Unknown provider: " + providerId);
    }
}
