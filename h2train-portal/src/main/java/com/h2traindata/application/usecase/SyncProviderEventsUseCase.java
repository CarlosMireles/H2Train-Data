package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.ConnectionNotFoundException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.bus.EventPublisher;
import com.h2traindata.application.port.out.ProviderCatalog;
import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.port.out.SyncStateRepository;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventPublication;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.domain.SyncState;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SyncProviderEventsUseCase {

    private final ProviderCatalog providerCatalog;
    private final ConnectionRepository connectionRepository;
    private final SyncStateRepository syncStateRepository;
    private final EventPublisher eventPublisher;

    public SyncProviderEventsUseCase(ProviderCatalog providerCatalog,
                                     ConnectionRepository connectionRepository,
                                     SyncStateRepository syncStateRepository,
                                     EventPublisher eventPublisher) {
        this.providerCatalog = providerCatalog;
        this.connectionRepository = connectionRepository;
        this.syncStateRepository = syncStateRepository;
        this.eventPublisher = eventPublisher;
    }

    public EventBatch execute(String providerId, String athleteId, EventType eventType, SyncCursor cursor) {
        ProviderConnection connection = connectionRepository.findByProviderAndAthlete(providerId, athleteId)
                .orElseThrow(() -> new ConnectionNotFoundException(providerId, athleteId));

        ProviderConnector connector = providerCatalog.connector(providerId);
        ProviderConnection activeConnection = refreshIfNeeded(connection, connector);
        SyncState syncState = syncStateRepository.findByProviderAndAthleteAndEventType(providerId, athleteId, eventType)
                .orElseGet(() -> legacySyncState(activeConnection, eventType));
        SyncCursor effectiveCursor = cursor != null ? cursor : syncState.lastCursor();
        EventBatch batch = providerCatalog.collector(providerId, eventType).collect(activeConnection, effectiveCursor);
        eventPublisher.publishAll(batch.events().stream()
                .map(event -> new EventPublication(activeConnection.userId(), event))
                .toList());
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
