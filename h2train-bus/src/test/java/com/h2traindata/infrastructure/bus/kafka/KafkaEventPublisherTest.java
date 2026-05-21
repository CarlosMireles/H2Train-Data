package com.h2traindata.infrastructure.bus.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2traindata.domain.BusEventEnvelope;
import com.h2traindata.domain.EventPublication;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderEvent;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

class KafkaEventPublisherTest {

    private final KafkaTemplate<String, BusEventEnvelope> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    private final KafkaEventProperties properties = new KafkaEventProperties();
    private final KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate, properties);

    @Test
    void publishesEnvelopeWithUserIdAsMessageKey() {
        properties.setTopic("h2train.events.v1");
        when(kafkaTemplate.send(eq("h2train.events.v1"), eq("internal-user-1"), any(BusEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        ProviderEvent event = new ProviderEvent(
                "strava",
                "99",
                EventType.ACTIVITY,
                "Workout",
                "321",
                Instant.parse("2026-04-03T10:15:30Z"),
                Map.of("activityType", "run")
        );

        publisher.publish(new EventPublication("internal-user-1", event));

        ArgumentCaptor<BusEventEnvelope> envelopeCaptor = ArgumentCaptor.forClass(BusEventEnvelope.class);
        verify(kafkaTemplate).send(eq("h2train.events.v1"), eq("internal-user-1"), envelopeCaptor.capture());
        assertEquals("internal-user-1", envelopeCaptor.getValue().userId());
        assertEquals("strava", envelopeCaptor.getValue().providerId());
        assertEquals("99", envelopeCaptor.getValue().athleteId());
        assertEquals("Workout", envelopeCaptor.getValue().eventName());
    }

    @Test
    void fallsBackToProviderAndAthleteAsMessageKeyWhenUserIdIsMissing() {
        properties.setTopic("h2train.events.v1");
        when(kafkaTemplate.send(eq("h2train.events.v1"), eq("fitbit:ABC123"), any(BusEventEnvelope.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        ProviderEvent event = new ProviderEvent(
                "fitbit",
                "ABC123",
                EventType.HEALTH,
                "Sleep",
                "ABC123:Sleep:1775682000000",
                Instant.parse("2026-04-08T21:00:00Z"),
                Map.of("duration", 28800)
        );

        publisher.publish(new EventPublication(null, event));

        verify(kafkaTemplate).send(eq("h2train.events.v1"), eq("fitbit:ABC123"), any(BusEventEnvelope.class));
    }
}
