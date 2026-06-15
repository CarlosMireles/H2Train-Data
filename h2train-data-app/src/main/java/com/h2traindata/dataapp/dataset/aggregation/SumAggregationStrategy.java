package com.h2traindata.dataapp.dataset.aggregation;

import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SumAggregationStrategy implements AggregationStrategy {

    @Override
    public DatasetAggregation type() {
        return DatasetAggregation.SUM;
    }

    @Override
    public Optional<BigDecimal> aggregate(List<TimeSeriesPoint> points) {
        List<BigDecimal> values = points.stream()
                .map(TimeSeriesPoint::value)
                .filter(value -> value != null)
                .toList();
        return values.isEmpty()
                ? Optional.empty()
                : Optional.of(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).stripTrailingZeros());
    }
}
