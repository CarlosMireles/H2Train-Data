package com.h2traindata.dataapp.dataset.aggregation;

import com.h2traindata.dataapp.dataset.exception.UnsupportedAggregationException;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DatasetAggregationService {

    private final Map<DatasetAggregation, AggregationStrategy> strategies;

    public DatasetAggregationService(List<AggregationStrategy> strategies) {
        Map<DatasetAggregation, AggregationStrategy> byType = new EnumMap<>(DatasetAggregation.class);
        strategies.forEach(strategy -> byType.put(strategy.type(), strategy));
        this.strategies = Map.copyOf(byType);
    }

    public Optional<BigDecimal> aggregate(DatasetAggregation aggregation, List<TimeSeriesPoint> points) {
        AggregationStrategy strategy = strategies.get(aggregation);
        if (strategy == null) {
            throw new UnsupportedAggregationException(aggregation == null ? null : aggregation.value());
        }
        return strategy.aggregate(points);
    }
}
