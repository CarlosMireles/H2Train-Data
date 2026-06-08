package com.h2traindata.dataapp.domain;

import java.time.Instant;
import java.util.List;

public record DatasetMetadata(
        int subjectCount,
        int metricCount,
        int pointCount,
        Instant firstRecord,
        Instant lastRecord,
        List<String> metrics
) {
}
