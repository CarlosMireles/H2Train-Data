package com.h2traindata.web.dto;

import com.h2traindata.domain.SyncInterval;
import java.time.Instant;

public record SyncSettingsResponse(
        String provider,
        String athleteId,
        String athleteUsername,
        boolean connected,
        boolean syncEnabled,
        SyncInterval syncInterval,
        String syncIntervalLabel,
        Instant lastSyncedAt
) {
}
