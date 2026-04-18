package com.h2traindata.application.usecase;

import static org.mockito.Mockito.verify;

import com.h2traindata.domain.EventType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SyncAllProviderEventsUseCaseTest {

    private final SyncProviderEventsUseCase syncProviderEventsUseCase = Mockito.mock(SyncProviderEventsUseCase.class);

    @Test
    void syncsEveryRegisteredEventTypeForTheProviderConnection() {
        SyncAllProviderEventsUseCase useCase = new SyncAllProviderEventsUseCase(syncProviderEventsUseCase);

        useCase.execute("strava", "7");

        verify(syncProviderEventsUseCase).execute("strava", "7", EventType.ACTIVITY, null);
        verify(syncProviderEventsUseCase).execute("strava", "7", EventType.USER_METRICS, null);
    }
}
