package com.h2traindata.dataapp.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ActivityRecord(
        String activityId,
        String userId,
        String provider,
        String activityType,
        Instant startTime,
        Instant endTime,
        BigDecimal duration,
        BigDecimal distanceMeters,
        BigDecimal calories,
        Instant updatedAt
) {
}
