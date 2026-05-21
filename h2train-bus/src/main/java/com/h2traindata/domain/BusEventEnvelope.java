package com.h2traindata.domain;

import java.time.Instant;
import java.util.UUID;

public record BusEventEnvelope(
        String messageId,
        String schemaVersion,
        Instant publishedAt,
        String userId,
        String providerId,
        String athleteId,
        EventType eventType,
        String eventName,
        String eventId,
        ProviderEvent event
) {
    public static final String CURRENT_SCHEMA_VERSION = "1";

    public static BusEventEnvelope from(EventPublication publication) {
        return from(publication, UUID.randomUUID().toString(), Instant.now());
    }

    public static BusEventEnvelope from(EventPublication publication, String messageId, Instant publishedAt) {
        ProviderEvent event = publication.event();
        return new BusEventEnvelope(
                messageId,
                CURRENT_SCHEMA_VERSION,
                publishedAt,
                publication.userId(),
                event.providerId(),
                event.athleteId(),
                event.eventType(),
                event.eventName(),
                event.eventId(),
                event
        );
    }
}
