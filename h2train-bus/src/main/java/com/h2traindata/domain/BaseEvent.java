package com.h2traindata.domain;

import java.time.Instant;

public record BaseEvent(
        Instant timestamp,
        String sourceSystem,
        String athleteId
) {
}
