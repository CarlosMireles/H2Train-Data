package com.h2traindata.dataapp.dataset.filter;

import com.h2traindata.dataapp.dataset.aggregation.DatasetAggregation;
import java.math.BigDecimal;

public record DatasetFilter(
        String metric,
        DatasetOperator operator,
        BigDecimal value,
        BigDecimal maxValue,
        DatasetAggregation aggregation,
        DatasetDimensions dimensions
) {
    public boolean matches(BigDecimal actual) {
        return operator.matches(actual, value, maxValue);
    }
}
