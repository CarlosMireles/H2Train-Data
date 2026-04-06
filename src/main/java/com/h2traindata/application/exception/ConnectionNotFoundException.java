package com.h2traindata.application.exception;

public class ConnectionNotFoundException extends RuntimeException {

    public ConnectionNotFoundException(String providerId, String athleteId) {
        super("No stored connection found for provider '%s' and athlete '%s'".formatted(providerId, athleteId));
    }
}
