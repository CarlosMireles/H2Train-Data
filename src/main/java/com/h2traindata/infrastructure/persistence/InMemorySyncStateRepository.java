package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.SyncStateRepository;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.SyncState;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySyncStateRepository implements SyncStateRepository {

    private final Map<SyncStateKey, SyncState> syncStates = new ConcurrentHashMap<>();

    @Override
    public Optional<SyncState> findByProviderAndAthleteAndEventType(String providerId,
                                                                    String athleteId,
                                                                    EventType eventType) {
        return Optional.ofNullable(syncStates.get(new SyncStateKey(providerId, athleteId, eventType)));
    }

    @Override
    public void save(SyncState syncState) {
        syncStates.put(new SyncStateKey(syncState.providerId(), syncState.athleteId(), syncState.eventType()), syncState);
    }

    private record SyncStateKey(String providerId, String athleteId, EventType eventType) {
    }
}
