package com.h2traindata.dataapp.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record NormalizedDatalakeEvent(
        String userId,
        String providerId,
        String athleteId,
        String eventType,
        String eventName,
        String eventId,
        Instant eventTimestamp,
        Instant publishedAt,
        JsonNode event
) {
}
