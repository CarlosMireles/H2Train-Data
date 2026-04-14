package com.h2traindata.application.service;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.usecase.SyncProviderEventsUseCase;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProviderSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProviderSyncScheduler.class);

    private final ConnectionRepository connectionRepository;
    private final SyncProviderEventsUseCase syncProviderEventsUseCase;

    public ProviderSyncScheduler(ConnectionRepository connectionRepository,
                                 SyncProviderEventsUseCase syncProviderEventsUseCase) {
        this.connectionRepository = connectionRepository;
        this.syncProviderEventsUseCase = syncProviderEventsUseCase;
    }

    @Scheduled(fixedDelayString = "${app.sync.poll-interval-ms:60000}")
    public void syncDueConnections() {
        Instant now = Instant.now();
        connectionRepository.findAll().stream()
                .filter(connection -> connection.isSyncDue(now))
                .forEach(this::syncSafely);
    }

    private void syncSafely(ProviderConnection connection) {
        try {
            syncProviderEventsUseCase.execute(
                    connection.providerId(),
                    connection.athlete().id(),
                    EventType.ACTIVITY,
                    null
            );
        } catch (RuntimeException exception) {
            log.warn("Scheduled sync failed for provider={} athlete={}",
                    connection.providerId(),
                    connection.athlete().id(),
                    exception);
        }
    }
}
