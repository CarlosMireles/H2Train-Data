package com.h2traindata.dataapp.domain;

public record TimeSeriesProjectionResult(
        boolean processed,
        boolean duplicate,
        int contributionCount
) {
    public static TimeSeriesProjectionResult duplicate(int contributionCount) {
        return new TimeSeriesProjectionResult(false, true, contributionCount);
    }

    public static TimeSeriesProjectionResult processed(int contributionCount) {
        return new TimeSeriesProjectionResult(true, false, contributionCount);
    }
}
