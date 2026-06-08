package com.h2traindata.dataapp.domain;

import java.time.Instant;

public record TimeSeriesContribution(
        TimeSeriesPoint point,
        boolean primaryDailyMetric,
        boolean fallbackDailyMetric,
        boolean applied,
        Instant sourceEventTimestamp
) {
    public TimeSeriesContribution asApplied(boolean value) {
        return new TimeSeriesContribution(point, primaryDailyMetric, fallbackDailyMetric, value, sourceEventTimestamp);
    }
}
