package com.h2traindata.application.usecase;

import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SyncAllProviderEventsUseCase {

    private final SyncProviderEventsUseCase syncProviderEventsUseCase;

    public SyncAllProviderEventsUseCase(SyncProviderEventsUseCase syncProviderEventsUseCase) {
        this.syncProviderEventsUseCase = syncProviderEventsUseCase;
    }

    public List<EventBatch> execute(String providerId, String athleteId) {
        return Arrays.stream(EventType.values())
                .map(eventType -> syncProviderEventsUseCase.execute(providerId, athleteId, eventType, null))
                .toList();
    }
}
