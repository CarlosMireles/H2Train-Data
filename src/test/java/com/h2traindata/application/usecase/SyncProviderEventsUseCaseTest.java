package com.h2traindata.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2traindata.application.exception.ProviderRateLimitException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.EventSink;
import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.application.port.out.SyncStateRepository;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.domain.SyncState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SyncProviderEventsUseCaseTest {

    private final ProviderConnector connector = Mockito.mock(ProviderConnector.class);
    private final ProviderEventCollector collector = Mockito.mock(ProviderEventCollector.class);
    private final ConnectionRepository connectionRepository = Mockito.mock(ConnectionRepository.class);
    private final SyncStateRepository syncStateRepository = Mockito.mock(SyncStateRepository.class);
    private final EventSink eventSink = Mockito.mock(EventSink.class);

    @Test
    void syncsStoredConnectionAndWritesBatchToSink() {
        when(connector.providerId()).thenReturn("strava");
        when(collector.providerId()).thenReturn("strava");
        when(collector.eventType()).thenReturn(EventType.ACTIVITY);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        SyncProviderEventsUseCase useCase =
                new SyncProviderEventsUseCase(providerRegistry, connectionRepository, syncStateRepository, eventSink);

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
        when(syncStateRepository.findByProviderAndAthleteAndEventType("strava", "7", EventType.ACTIVITY))
                .thenReturn(Optional.empty());
        when(collector.collect(connection, null)).thenReturn(batch);

        EventBatch result = useCase.execute("strava", "7", EventType.ACTIVITY, null);

        assertEquals(1, result.events().size());
        verify(eventSink).write(batch);
        verify(connectionRepository).save(argThat(savedConnection ->
                savedConnection.lastSyncedAt() != null && savedConnection.lastSyncCursor() == null
        ));
    }

    @Test
    void reusesStoredCursorWhenNoCursorIsProvided() {
        when(connector.providerId()).thenReturn("strava");
        when(collector.providerId()).thenReturn("strava");
        when(collector.eventType()).thenReturn(EventType.ACTIVITY);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        SyncProviderEventsUseCase useCase =
                new SyncProviderEventsUseCase(providerRegistry, connectionRepository, syncStateRepository, eventSink);

        ProviderConnection connection = new ProviderConnection(
                "strava",
                new AthleteProfile("7", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(300),
                null,
                new SyncCursor("1700000000"),
                Instant.parse("2026-04-01T09:00:00Z")
        );
        EventBatch batch = new EventBatch(
                "strava",
                "7",
                EventType.ACTIVITY,
                List.of(),
                new SyncCursor("1700003600")
        );

        when(connectionRepository.findByProviderAndAthlete("strava", "7")).thenReturn(Optional.of(connection));
        when(syncStateRepository.findByProviderAndAthleteAndEventType("strava", "7", EventType.ACTIVITY))
                .thenReturn(Optional.empty());
        when(collector.collect(connection, new SyncCursor("1700000000"))).thenReturn(batch);

        useCase.execute("strava", "7", EventType.ACTIVITY, null);

        verify(connectionRepository).save(argThat(savedConnection ->
                savedConnection.lastSyncCursor() != null
                        && "1700003600".equals(savedConnection.lastSyncCursor().value())
                        && savedConnection.lastSyncedAt() != null
        ));
    }

    @Test
    void keepsExistingActivityCursorWhenSnapshotEventReturnsNoCursor() {
        when(connector.providerId()).thenReturn("strava");
        when(collector.providerId()).thenReturn("strava");
        when(collector.eventType()).thenReturn(EventType.USER_METRICS);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        SyncProviderEventsUseCase useCase =
                new SyncProviderEventsUseCase(providerRegistry, connectionRepository, syncStateRepository, eventSink);

        ProviderConnection connection = new ProviderConnection(
                "strava",
                new AthleteProfile("7", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(300),
                null,
                new SyncCursor("1700000000"),
                Instant.parse("2026-04-01T09:00:00Z")
        );
        EventBatch batch = new EventBatch(
                "strava",
                "7",
                EventType.USER_METRICS,
                List.of(),
                null
        );

        when(connectionRepository.findByProviderAndAthlete("strava", "7")).thenReturn(Optional.of(connection));
        when(syncStateRepository.findByProviderAndAthleteAndEventType("strava", "7", EventType.USER_METRICS))
                .thenReturn(Optional.empty());
        when(collector.collect(connection, null)).thenReturn(batch);

        useCase.execute("strava", "7", EventType.USER_METRICS, null);

        verify(connectionRepository).save(argThat(savedConnection ->
                savedConnection.lastSyncCursor() != null
                        && "1700000000".equals(savedConnection.lastSyncCursor().value())
                        && savedConnection.lastSyncedAt() != null
        ));
        verify(syncStateRepository).save(argThat(savedState ->
                savedState.lastCursor() == null && savedState.lastSyncedAt() != null
        ));
    }

    @Test
    void doesNotMarkSyncSuccessfulWhenProviderRateLimitIsReached() {
        when(connector.providerId()).thenReturn("fitbit");
        when(collector.providerId()).thenReturn("fitbit");
        when(collector.eventType()).thenReturn(EventType.ACTIVITY);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        SyncProviderEventsUseCase useCase =
                new SyncProviderEventsUseCase(providerRegistry, connectionRepository, syncStateRepository, eventSink);

        ProviderConnection connection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("10", "walker"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(300)
        );

        when(connectionRepository.findByProviderAndAthlete("fitbit", "10")).thenReturn(Optional.of(connection));
        when(syncStateRepository.findByProviderAndAthleteAndEventType("fitbit", "10", EventType.ACTIVITY))
                .thenReturn(Optional.empty());
        when(collector.collect(connection, null)).thenThrow(new ProviderRateLimitException(
                "fitbit",
                "fetch Fitbit activity logs",
                120L,
                null
        ));

        assertThrows(ProviderRateLimitException.class, () -> useCase.execute("fitbit", "10", EventType.ACTIVITY, null));

        verify(eventSink, never()).write(org.mockito.ArgumentMatchers.any());
        verify(connectionRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(syncStateRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void prefersPerEventTypeSyncStateOverLegacyConnectionCursor() {
        when(connector.providerId()).thenReturn("strava");
        when(collector.providerId()).thenReturn("strava");
        when(collector.eventType()).thenReturn(EventType.ACTIVITY);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        SyncProviderEventsUseCase useCase =
                new SyncProviderEventsUseCase(providerRegistry, connectionRepository, syncStateRepository, eventSink);

        ProviderConnection connection = new ProviderConnection(
                "strava",
                new AthleteProfile("7", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(300),
                null,
                new SyncCursor("legacy-cursor"),
                Instant.parse("2026-04-01T09:00:00Z")
        );
        SyncState syncState = new SyncState(
                "strava",
                "7",
                EventType.ACTIVITY,
                new SyncCursor("state-cursor"),
                Instant.parse("2026-04-02T09:00:00Z")
        );
        EventBatch batch = new EventBatch(
                "strava",
                "7",
                EventType.ACTIVITY,
                List.of(),
                new SyncCursor("next-state-cursor")
        );

        when(connectionRepository.findByProviderAndAthlete("strava", "7")).thenReturn(Optional.of(connection));
        when(syncStateRepository.findByProviderAndAthleteAndEventType("strava", "7", EventType.ACTIVITY))
                .thenReturn(Optional.of(syncState));
        when(collector.collect(connection, new SyncCursor("state-cursor"))).thenReturn(batch);

        useCase.execute("strava", "7", EventType.ACTIVITY, null);

        verify(connectionRepository).save(argThat(savedConnection ->
                savedConnection.lastSyncCursor() != null
                        && "next-state-cursor".equals(savedConnection.lastSyncCursor().value())
        ));
    }
}
