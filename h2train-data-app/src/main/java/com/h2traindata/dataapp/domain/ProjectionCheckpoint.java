package com.h2traindata.dataapp.domain;

import java.time.Instant;

public record ProjectionCheckpoint(
        String projectionName,
        String source,
        String channel,
        Integer partition,
        Long offset,
        Instant processedAt
) {
}
