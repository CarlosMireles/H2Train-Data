package com.h2traindata.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.EventType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SyncAllProviderEventsUseCaseTest {

    private final SyncProviderEventsUseCase syncProviderEventsUseCase = Mockito.mock(SyncProviderEventsUseCase.class);
    private final ProviderConnector connector = Mockito.mock(ProviderConnector.class);
    private final ProviderEventCollector userStateCollector = Mockito.mock(ProviderEventCollector.class);
    private final ProviderEventCollector activityCollector = Mockito.mock(ProviderEventCollector.class);

    @Test
    void syncsEverySupportedEventTypeForTheProviderConnection() {
        when(connector.providerId()).thenReturn("strava");
        when(userStateCollector.providerId()).thenReturn("strava");
        when(userStateCollector.eventType()).thenReturn(EventType.USER_STATE);
        when(activityCollector.providerId()).thenReturn("strava");
        when(activityCollector.eventType()).thenReturn(EventType.ACTIVITY);

        ProviderRegistry providerRegistry = new ProviderRegistry(
                List.of(connector),
                List.of(userStateCollector, activityCollector)
        );
        SyncAllProviderEventsUseCase useCase = new SyncAllProviderEventsUseCase(providerRegistry, syncProviderEventsUseCase);

        useCase.execute("strava", "7");

        verify(syncProviderEventsUseCase).execute("strava", "7", EventType.USER_STATE, null);
        verify(syncProviderEventsUseCase).execute("strava", "7", EventType.ACTIVITY, null);
    }
}
