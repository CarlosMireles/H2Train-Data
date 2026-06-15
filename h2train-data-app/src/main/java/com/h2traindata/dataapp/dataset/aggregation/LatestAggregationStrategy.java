package com.h2traindata.dataapp.dataset.aggregation;

import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LatestAggregationStrategy implements AggregationStrategy {

    @Override
    public DatasetAggregation type() {
        return DatasetAggregation.LATEST;
    }

    @Override
    public Optional<BigDecimal> aggregate(List<TimeSeriesPoint> points) {
        return points.stream()
                .filter(point -> point.value() != null)
                .filter(point -> point.periodStart() != null)
                .max(Comparator.comparing(TimeSeriesPoint::periodStart))
                .map(TimeSeriesPoint::value)
                .map(BigDecimal::stripTrailingZeros);
    }
}
