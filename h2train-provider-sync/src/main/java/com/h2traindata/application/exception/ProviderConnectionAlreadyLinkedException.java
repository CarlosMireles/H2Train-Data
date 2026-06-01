package com.h2traindata.application.exception;

public class ProviderConnectionAlreadyLinkedException extends RuntimeException {

    public ProviderConnectionAlreadyLinkedException(String providerId, String athleteId) {
        super("Provider account '%s/%s' is already linked to another H2Train user".formatted(providerId, athleteId));
    }
}
