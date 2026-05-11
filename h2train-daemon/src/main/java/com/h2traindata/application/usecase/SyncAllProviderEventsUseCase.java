package com.h2traindata.application.usecase;

import com.h2traindata.domain.EventBatch;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SyncAllProviderEventsUseCase {

    private final com.h2traindata.application.service.ProviderRegistry providerRegistry;
    private final SyncProviderEventsUseCase syncProviderEventsUseCase;

    public SyncAllProviderEventsUseCase(com.h2traindata.application.service.ProviderRegistry providerRegistry,
                                        SyncProviderEventsUseCase syncProviderEventsUseCase) {
        this.providerRegistry = providerRegistry;
        this.syncProviderEventsUseCase = syncProviderEventsUseCase;
    }

    public List<EventBatch> execute(String providerId, String athleteId) {
        return providerRegistry.supportedEventTypes(providerId).stream()
                .map(eventType -> syncProviderEventsUseCase.execute(providerId, athleteId, eventType, null))
                .toList();
    }
}
