package com.h2traindata.dataapp.domain;

import java.util.List;

public record TimeSeriesProjection(
        NormalizedDatalakeEvent sourceEvent,
        String sourceKey,
        String payloadHash,
        List<TimeSeriesContribution> contributions
) {
}
