package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.ConnectionNotFoundException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.EventSink;
import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncCursor;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SyncProviderEventsUseCase {

    private final ProviderRegistry providerRegistry;
    private final ConnectionRepository connectionRepository;
    private final EventSink eventSink;

    public SyncProviderEventsUseCase(ProviderRegistry providerRegistry,
                                     ConnectionRepository connectionRepository,
                                     EventSink eventSink) {
        this.providerRegistry = providerRegistry;
        this.connectionRepository = connectionRepository;
        this.eventSink = eventSink;
    }

    public EventBatch execute(String providerId, String athleteId, EventType eventType, SyncCursor cursor) {
        ProviderConnection connection = connectionRepository.findByProviderAndAthlete(providerId, athleteId)
                .orElseThrow(() -> new ConnectionNotFoundException(providerId, athleteId));

        ProviderConnector connector = providerRegistry.connector(providerId);
        ProviderConnection activeConnection = refreshIfNeeded(connection, connector);
        SyncCursor effectiveCursor = cursor != null ? cursor : activeConnection.lastSyncCursor();
        EventBatch batch = providerRegistry.collector(providerId, eventType).collect(activeConnection, effectiveCursor);
        eventSink.write(batch);
        connectionRepository.save(activeConnection.withSuccessfulSync(batch.nextCursor(), Instant.now()));
        return batch;
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
