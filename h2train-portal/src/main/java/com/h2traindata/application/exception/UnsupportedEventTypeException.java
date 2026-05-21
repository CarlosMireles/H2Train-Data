package com.h2traindata.application.exception;

import com.h2traindata.domain.EventType;

public class UnsupportedEventTypeException extends RuntimeException {

    public UnsupportedEventTypeException(String providerId, EventType eventType) {
        super("Provider '%s' does not support event type '%s'".formatted(providerId, eventType));
    }
}
