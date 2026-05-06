package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.ConnectionNotFoundException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.EventSink;
import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.port.out.SyncStateRepository;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.domain.SyncState;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SyncProviderEventsUseCase {

    private final ProviderRegistry providerRegistry;
    private final ConnectionRepository connectionRepository;
    private final SyncStateRepository syncStateRepository;
    private final EventSink eventSink;

    public SyncProviderEventsUseCase(ProviderRegistry providerRegistry,
                                     ConnectionRepository connectionRepository,
                                     SyncStateRepository syncStateRepository,
                                     EventSink eventSink) {
        this.providerRegistry = providerRegistry;
        this.connectionRepository = connectionRepository;
        this.syncStateRepository = syncStateRepository;
        this.eventSink = eventSink;
    }

    public EventBatch execute(String providerId, String athleteId, EventType eventType, SyncCursor cursor) {
        ProviderConnection connection = connectionRepository.findByProviderAndAthlete(providerId, athleteId)
                .orElseThrow(() -> new ConnectionNotFoundException(providerId, athleteId));

        ProviderConnector connector = providerRegistry.connector(providerId);
        ProviderConnection activeConnection = refreshIfNeeded(connection, connector);
        SyncState syncState = syncStateRepository.findByProviderAndAthleteAndEventType(providerId, athleteId, eventType)
                .orElseGet(() -> legacySyncState(activeConnection, eventType));
        SyncCursor effectiveCursor = cursor != null ? cursor : syncState.lastCursor();
        EventBatch batch = providerRegistry.collector(providerId, eventType).collect(activeConnection, effectiveCursor);
        eventSink.write(batch);
        Instant syncedAt = Instant.now();
        SyncState updatedSyncState = syncState.withSuccessfulSync(batch.nextCursor(), syncedAt);
        syncStateRepository.save(updatedSyncState);
        connectionRepository.save(activeConnection.withSuccessfulSync(
                mirroredConnectionCursor(activeConnection, eventType, updatedSyncState),
                syncedAt
        ));
        return batch;
    }

    private SyncState legacySyncState(ProviderConnection connection, EventType eventType) {
        return new SyncState(
                connection.providerId(),
                connection.athlete().id(),
                eventType,
                eventType.usesCursor() ? connection.lastSyncCursor() : null,
                connection.lastSyncedAt()
        );
    }

    private SyncCursor mirroredConnectionCursor(ProviderConnection connection, EventType eventType, SyncState syncState) {
        if (!eventType.usesCursor()) {
            return connection.lastSyncCursor();
        }
        return syncState.lastCursor();
    }

    private ProviderConnection refreshIfNeeded(ProviderConnection connection, ProviderConnector connector) {
        if (connection.refreshToken() == null || connection.expiresAt() == null) {
            return connection;
        }

        if (connection.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return connection;
        }

        ProviderConnection refreshedConnection = connector.refresh(connection);
        connectionRepository.save(refreshedConnection);
        return refreshedConnection;
    }
}
