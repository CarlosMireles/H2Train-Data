package com.h2traindata.dataapp.dataset.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.h2traindata.dataapp.domain.AggregationType;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AggregationStrategyTest {

    private final List<TimeSeriesPoint> points = List.of(
            point("2026-01-01T00:00:00Z", "10"),
            point("2026-01-02T00:00:00Z", "20"),
            point("2026-01-03T00:00:00Z", "30")
    );

    @Test
    void calculatesAverage() {
        assertAggregation("20", new AverageAggregationStrategy());
    }

    @Test
    void calculatesSum() {
        assertAggregation("60", new SumAggregationStrategy());
    }

    @Test
    void calculatesMin() {
        assertAggregation("10", new MinAggregationStrategy());
    }

    @Test
    void calculatesMax() {
        assertAggregation("30", new MaxAggregationStrategy());
    }

    @Test
    void calculatesCount() {
        assertAggregation("3", new CountAggregationStrategy());
    }

    @Test
    void calculatesLatest() {
        assertAggregation("30", new LatestAggregationStrategy());
    }

    private void assertAggregation(String expected, AggregationStrategy strategy) {
        BigDecimal actual = strategy.aggregate(points).orElseThrow();
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private TimeSeriesPoint point(String startValue, String value) {
        Instant start = Instant.parse(startValue);
        return new TimeSeriesPoint(
                "u001",
                "metric",
                "P1D",
                start,
                start.plusSeconds(86400),
                new BigDecimal(value),
                "unit",
                "test",
                null,
                null,
                AggregationType.SUM,
                null,
                null,
                start
        );
    }
}
