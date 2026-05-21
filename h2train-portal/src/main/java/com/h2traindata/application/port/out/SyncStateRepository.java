package com.h2traindata.application.port.out;

import com.h2traindata.domain.EventType;
import com.h2traindata.domain.SyncState;
import java.util.Optional;

public interface SyncStateRepository {

    Optional<SyncState> findByProviderAndAthleteAndEventType(String providerId, String athleteId, EventType eventType);

    void save(SyncState syncState);
}
