package com.h2traindata.dataapp.domain;

import com.h2traindata.domain.EventType;
import java.time.Instant;
import java.util.Optional;

public record DatalakeReadRequest(
        Optional<String> providerId,
        Optional<EventType> eventType,
        Optional<Instant> from,
        Optional<Instant> to
) {
    public static DatalakeReadRequest all() {
        return new DatalakeReadRequest(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
