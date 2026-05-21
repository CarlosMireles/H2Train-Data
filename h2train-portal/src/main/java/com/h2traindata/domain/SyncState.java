package com.h2traindata.domain;

import java.time.Instant;

public record SyncState(
        String providerId,
        String athleteId,
        EventType eventType,
        SyncCursor lastCursor,
        Instant lastSyncedAt
) {

    public SyncState withSuccessfulSync(SyncCursor nextCursor, Instant syncedAt) {
        return new SyncState(
                providerId,
                athleteId,
                eventType,
                nextCursor != null ? nextCursor : lastCursor,
                syncedAt
        );
    }
}
