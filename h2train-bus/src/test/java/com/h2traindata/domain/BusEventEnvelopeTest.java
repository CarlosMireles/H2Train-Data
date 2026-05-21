package com.h2traindata.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BusEventEnvelopeTest {

    @Test
    void createsEnvelopeFromPublication() {
        ProviderEvent event = new ProviderEvent(
                "strava",
                "99",
                EventType.ACTIVITY,
                "Workout",
                "321",
                Instant.parse("2026-04-03T10:15:30Z"),
                Map.of("activityType", "run")
        );

        BusEventEnvelope envelope = BusEventEnvelope.from(
                new EventPublication("internal-user-1", event),
                "message-1",
                Instant.parse("2026-05-12T10:00:00Z")
        );

        assertEquals("message-1", envelope.messageId());
        assertEquals("1", envelope.schemaVersion());
        assertEquals("internal-user-1", envelope.userId());
        assertEquals("strava", envelope.providerId());
        assertEquals("99", envelope.athleteId());
        assertEquals(EventType.ACTIVITY, envelope.eventType());
        assertEquals("Workout", envelope.eventName());
        assertEquals("321", envelope.eventId());
        assertEquals(event, envelope.event());
    }
}
