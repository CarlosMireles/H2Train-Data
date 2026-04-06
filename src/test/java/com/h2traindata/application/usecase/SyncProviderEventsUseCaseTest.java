package com.h2traindata.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.EventSink;
import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SyncProviderEventsUseCaseTest {

    private final ProviderConnector connector = Mockito.mock(ProviderConnector.class);
    private final ProviderEventCollector collector = Mockito.mock(ProviderEventCollector.class);
    private final ConnectionRepository connectionRepository = Mockito.mock(ConnectionRepository.class);
    private final EventSink eventSink = Mockito.mock(EventSink.class);

    @Test
    void syncsStoredConnectionAndWritesBatchToSink() {
        when(connector.providerId()).thenReturn("strava");
        when(collector.providerId()).thenReturn("strava");
        when(collector.eventType()).thenReturn(EventType.ACTIVITY);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        SyncProviderEventsUseCase useCase =
                new SyncProviderEventsUseCase(providerRegistry, connectionRepository, eventSink);

        ProviderConnection connection = new ProviderConnection(
                "strava",
                new AthleteProfile("7", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(300)
        );
        EventBatch batch = new EventBatch(
                "strava",
                "7",
                EventType.ACTIVITY,
                List.of(new ProviderEvent(
                        "strava",
                        "7",
                        EventType.ACTIVITY,
                        "123",
                        Instant.parse("2026-04-03T10:15:30Z"),
                        java.util.Map.of("name", "Morning Ride"),
                        java.util.Map.of("kudosCount", 3),
                        java.util.Map.of("id", 123L)
                )),
                null
        );

        when(connectionRepository.findByProviderAndAthlete("strava", "7")).thenReturn(Optional.of(connection));
        when(collector.collect(connection, null)).thenReturn(batch);

        EventBatch result = useCase.execute("strava", "7", EventType.ACTIVITY, null);

        assertEquals(1, result.events().size());
        verify(eventSink).write(batch);
    }
}
