package com.h2traindata.dataapp.dataset.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record DatasetExportRow(
        String userId,
        String metric,
        BigDecimal value,
        Instant periodStart,
        Instant periodEnd,
        String unit,
        String period,
        String provider,
        String activityType,
        String zone
) {
}
