package com.h2traindata.datalake.domain;

import java.time.Instant;

public record DatalakeEventRecord(
        String rawJson,
        String userId,
        String providerId,
        String eventType,
        Instant eventTimestamp
) {
}
