package com.h2traindata.application.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.usecase.SyncAllProviderEventsUseCase;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncInterval;
import com.h2traindata.domain.SyncPreferences;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProviderSyncSchedulerTest {

    private final ConnectionRepository connectionRepository = Mockito.mock(ConnectionRepository.class);
    private final SyncAllProviderEventsUseCase syncAllProviderEventsUseCase = Mockito.mock(SyncAllProviderEventsUseCase.class);
    private final java.util.concurrent.Executor directExecutor = command -> command.run();

    @Test
    void syncsOnlyConnectionsThatAreEnabledAndDue() {
        ProviderSyncScheduler scheduler = new ProviderSyncScheduler(
                connectionRepository,
                syncAllProviderEventsUseCase,
                directExecutor
        );
        ProviderConnection dueDailyConnection = new ProviderConnection(
                "strava",
                new AthleteProfile("7", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600),
                new SyncPreferences(true, SyncInterval.EVERY_24_HOURS),
                null,
                Instant.now().minus(SyncInterval.EVERY_24_HOURS.duration()).minusSeconds(60)
        );
        ProviderConnection dueFiveHourConnection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("10", "sprinter"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600),
                new SyncPreferences(true, SyncInterval.EVERY_5_HOURS),
                null,
                Instant.now().minus(SyncInterval.EVERY_5_HOURS.duration()).minusSeconds(60)
        );
        ProviderConnection disabledConnection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("8", "walker"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600),
                new SyncPreferences(false, SyncInterval.EVERY_24_HOURS),
                null,
                Instant.now().minus(SyncInterval.EVERY_24_HOURS.duration()).minusSeconds(60)
        );
        ProviderConnection notDueConnection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("9", "cyclist"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600),
                new SyncPreferences(true, SyncInterval.EVERY_7_DAYS),
                null,
                Instant.now().minusSeconds(3600)
        );

        when(connectionRepository.findAll()).thenReturn(List.of(
                dueDailyConnection,
                dueFiveHourConnection,
                disabledConnection,
                notDueConnection
        ));

        scheduler.syncDueConnections();

        verify(syncAllProviderEventsUseCase).execute("strava", "7");
        verify(syncAllProviderEventsUseCase).execute("fitbit", "10");
        verify(syncAllProviderEventsUseCase, never()).execute("fitbit", "8");
        verify(syncAllProviderEventsUseCase, never()).execute("fitbit", "9");
    }
}
