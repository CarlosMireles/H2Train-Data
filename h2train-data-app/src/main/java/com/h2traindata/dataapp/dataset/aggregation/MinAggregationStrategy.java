package com.h2traindata.dataapp.dataset.aggregation;

import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MinAggregationStrategy implements AggregationStrategy {

    @Override
    public DatasetAggregation type() {
        return DatasetAggregation.MIN;
    }

    @Override
    public Optional<BigDecimal> aggregate(List<TimeSeriesPoint> points) {
        return points.stream()
                .map(TimeSeriesPoint::value)
                .filter(value -> value != null)
                .min(BigDecimal::compareTo)
                .map(BigDecimal::stripTrailingZeros);
    }
}
