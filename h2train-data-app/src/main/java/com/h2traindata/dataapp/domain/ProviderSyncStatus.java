package com.h2traindata.dataapp.domain;

import java.time.Instant;

public record ProviderSyncStatus(
        String provider,
        Boolean syncEnabled,
        String syncInterval,
        Instant lastSync,
        String status
) {
}
