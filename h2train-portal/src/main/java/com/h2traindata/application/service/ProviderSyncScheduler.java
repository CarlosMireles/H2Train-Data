package com.h2traindata.application.service;

import com.h2traindata.application.exception.ProviderRateLimitException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.usecase.SyncAllProviderEventsUseCase;
import com.h2traindata.domain.ProviderConnection;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProviderSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProviderSyncScheduler.class);

    private final ConnectionRepository connectionRepository;
    private final SyncAllProviderEventsUseCase syncAllProviderEventsUseCase;
    private final Executor connectionSyncExecutor;

    public ProviderSyncScheduler(ConnectionRepository connectionRepository,
                                 SyncAllProviderEventsUseCase syncAllProviderEventsUseCase,
                                 @Qualifier("connectionSyncExecutor") Executor connectionSyncExecutor) {
        this.connectionRepository = connectionRepository;
        this.syncAllProviderEventsUseCase = syncAllProviderEventsUseCase;
        this.connectionSyncExecutor = connectionSyncExecutor;
    }

    @Scheduled(fixedDelayString = "${app.sync.poll-interval-ms:60000}")
    public void syncDueConnections() {
        Instant now = Instant.now();
        List<CompletableFuture<Void>> syncTasks = connectionRepository.findAll().stream()
                .filter(connection -> connection.isSyncDue(now))
                .map(connection -> CompletableFuture.runAsync(() -> syncSafely(connection), connectionSyncExecutor))
                .toList();

        syncTasks.forEach(CompletableFuture::join);
    }

    private void syncSafely(ProviderConnection connection) {
        try {
            syncAllProviderEventsUseCase.execute(connection.providerId(), connection.athlete().id());
        } catch (ProviderRateLimitException exception) {
            log.warn("Scheduled sync rate-limited for provider={} athlete={} operation={} retryAfterSeconds={}",
                    connection.providerId(),
                    connection.athlete().id(),
                    exception.operation(),
                    exception.retryAfterSeconds());
        } catch (RuntimeException exception) {
            log.warn("Scheduled sync failed for provider={} athlete={}",
                    connection.providerId(),
                    connection.athlete().id(),
                    exception);
        }
    }
}
