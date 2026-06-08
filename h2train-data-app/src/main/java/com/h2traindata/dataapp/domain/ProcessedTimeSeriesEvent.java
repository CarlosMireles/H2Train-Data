package com.h2traindata.dataapp.domain;

import java.util.List;

public record ProcessedTimeSeriesEvent(
        String sourceKey,
        String payloadHash,
        String userId,
        List<TimeSeriesContribution> contributions
) {
}
