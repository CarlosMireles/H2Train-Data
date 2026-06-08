package com.h2traindata.dataapp.domain;

import java.time.Instant;

public record SyncHistoryEntry(
        String syncId,
        String userId,
        String provider,
        Boolean syncEnabled,
        String syncInterval,
        Instant syncedAt,
        String status
) {
}
