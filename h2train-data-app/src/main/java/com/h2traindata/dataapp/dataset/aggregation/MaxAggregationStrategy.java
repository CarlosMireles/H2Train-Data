package com.h2traindata.dataapp.dataset.aggregation;

import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MaxAggregationStrategy implements AggregationStrategy {

    @Override
    public DatasetAggregation type() {
        return DatasetAggregation.MAX;
    }

    @Override
    public Optional<BigDecimal> aggregate(List<TimeSeriesPoint> points) {
        return points.stream()
                .map(TimeSeriesPoint::value)
                .filter(value -> value != null)
                .max(BigDecimal::compareTo)
                .map(BigDecimal::stripTrailingZeros);
    }
}
