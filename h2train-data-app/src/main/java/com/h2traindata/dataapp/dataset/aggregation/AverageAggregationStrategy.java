package com.h2traindata.dataapp.dataset.aggregation;

import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AverageAggregationStrategy implements AggregationStrategy {

    @Override
    public DatasetAggregation type() {
        return DatasetAggregation.AVG;
    }

    @Override
    public Optional<BigDecimal> aggregate(List<TimeSeriesPoint> points) {
        List<BigDecimal> values = values(points);
        if (values.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(sum.divide(BigDecimal.valueOf(values.size()), MathContext.DECIMAL128)
                .stripTrailingZeros());
    }

    private List<BigDecimal> values(List<TimeSeriesPoint> points) {
        return points.stream().map(TimeSeriesPoint::value).filter(value -> value != null).toList();
    }
}
