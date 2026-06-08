package com.h2traindata.dataapp.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record SubjectInfo(
        String subjectId,
        String userId,
        Set<String> providers,
        String timezone,
        String gender,
        BigDecimal height,
        BigDecimal weight,
        Instant firstRecord,
        Instant lastRecord
) {
}
