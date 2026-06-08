package com.h2traindata.daemon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2traindata.application.exception.ProviderAuthorizationException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.usecase.SyncAllProviderEventsUseCase;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncInterval;
import com.h2traindata.domain.SyncPreferences;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ProviderSyncSchedulerTest {

    private final ConnectionRepository connectionRepository = Mockito.mock(ConnectionRepository.class);
    private final SyncAllProviderEventsUseCase syncAllProviderEventsUseCase = Mockito.mock(SyncAllProviderEventsUseCase.class);
    private final java.util.concurrent.Executor directExecutor = command -> command.run();

    @BeforeEach
    void resetMocks() {
        Mockito.reset(connectionRepository, syncAllProviderEventsUseCase);
    }

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

    @Test
    void pausesAutomaticSyncWhenProviderAuthorizationIsInvalid() {
        ProviderSyncScheduler scheduler = new ProviderSyncScheduler(
                connectionRepository,
                syncAllProviderEventsUseCase,
                directExecutor
        );
        ProviderConnection dueConnection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("10", "sprinter"),
                "access-token",
                "refresh-token",
                Instant.now().minusSeconds(60),
                new SyncPreferences(true, SyncInterval.EVERY_5_HOURS),
                null,
                Instant.now().minus(SyncInterval.EVERY_5_HOURS.duration()).minusSeconds(60)
        );

        when(connectionRepository.findAll()).thenReturn(List.of(dueConnection));
        Mockito.doThrow(new ProviderAuthorizationException("fitbit", "refresh an access token"))
                .when(syncAllProviderEventsUseCase)
                .execute("fitbit", "10");

        scheduler.syncDueConnections();

        ArgumentCaptor<ProviderConnection> captor = ArgumentCaptor.forClass(ProviderConnection.class);
        verify(connectionRepository).save(captor.capture());
        ProviderConnection pausedConnection = captor.getValue();
        assertFalse(pausedConnection.syncPreferences().enabled());
        assertEquals(SyncInterval.EVERY_5_HOURS, pausedConnection.syncPreferences().interval());
    }
}
