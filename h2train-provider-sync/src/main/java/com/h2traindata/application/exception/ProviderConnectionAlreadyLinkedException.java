package com.h2traindata.application.exception;

public class ProviderConnectionAlreadyLinkedException extends RuntimeException {

    private final String providerId;
    private final String athleteId;

    public ProviderConnectionAlreadyLinkedException(String providerId, String athleteId) {
        super("Provider account '%s/%s' is already linked to another H2Train user".formatted(providerId, athleteId));
        this.providerId = providerId;
        this.athleteId = athleteId;
    }

    public String providerId() {
        return providerId;
    }

    public String athleteId() {
        return athleteId;
    }
}
