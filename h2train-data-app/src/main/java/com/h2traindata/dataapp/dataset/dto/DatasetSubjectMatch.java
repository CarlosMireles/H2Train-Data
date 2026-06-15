package com.h2traindata.dataapp.dataset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatasetSubjectMatch(
        String userId,
        String metric,
        BigDecimal aggregatedValue,
        LocalDate from,
        LocalDate to
) {
}
