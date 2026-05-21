package com.h2traindata.application.usecase;

import com.h2traindata.application.port.out.ProviderCatalog;
import com.h2traindata.domain.EventBatch;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SyncAllProviderEventsUseCase {

    private final ProviderCatalog providerCatalog;
    private final SyncProviderEventsUseCase syncProviderEventsUseCase;

    public SyncAllProviderEventsUseCase(ProviderCatalog providerCatalog,
                                        SyncProviderEventsUseCase syncProviderEventsUseCase) {
        this.providerCatalog = providerCatalog;
        this.syncProviderEventsUseCase = syncProviderEventsUseCase;
    }

    public List<EventBatch> execute(String providerId, String athleteId) {
        return providerCatalog.supportedEventTypes(providerId).stream()
                .map(eventType -> syncProviderEventsUseCase.execute(providerId, athleteId, eventType, null))
                .toList();
    }
}
