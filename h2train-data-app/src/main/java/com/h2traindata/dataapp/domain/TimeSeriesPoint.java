package com.h2traindata.dataapp.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TimeSeriesPoint(
        String userId,
        String metricName,
        String period,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal value,
        String unit,
        String provider,
        String sourceEventType,
        String sourceEventName,
        AggregationType aggregationType,
        String activityType,
        String zone,
        Instant generatedAt
) {
}
