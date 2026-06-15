package com.h2traindata.dataapp.dataset.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DatasetFilterRequest(
        String metric,
        String operator,
        BigDecimal value,
        BigDecimal maxValue,
        String aggregation,
        Map<String, List<String>> dimensions
) {
}
