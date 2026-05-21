package com.h2traindata.dataapp.domain;

import com.h2traindata.domain.BusEventEnvelope;
import com.h2traindata.domain.EventType;
import java.time.Instant;

public record DataMartProjection(
        String userId,
        String providerId,
        String athleteId,
        EventType eventType,
        String eventName,
        String eventId,
        Instant eventTimestamp,
        BusEventEnvelope sourceEnvelope
) {
}
