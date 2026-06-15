package com.h2traindata.dataapp.dataset.aggregation;

import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AggregationStrategy {

    DatasetAggregation type();

    Optional<BigDecimal> aggregate(List<TimeSeriesPoint> points);
}
