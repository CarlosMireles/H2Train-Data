package com.h2traindata.dataapp.dataset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.h2traindata.dataapp.dataset.aggregation.AverageAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.CountAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.DatasetAggregationService;
import com.h2traindata.dataapp.dataset.aggregation.LatestAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.MaxAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.MinAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.SumAggregationStrategy;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryRequest;
import com.h2traindata.dataapp.dataset.exception.UnsupportedDatasetFormatException;
import com.h2traindata.dataapp.dataset.exception.UnsupportedMetricException;
import com.h2traindata.dataapp.domain.AggregationType;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasetQueryServiceTest {

    private InMemoryReader reader;
    private DatasetQueryService service;

    @BeforeEach
    void setUp() {
        reader = new InMemoryReader();
        reader.addSubject("u001");
        reader.addSubject("u002");
        reader.add("u001", point("u001", "daily_calories", "2026-01-01T00:00:00Z", "2400"));
        reader.add("u001", point("u001", "daily_calories", "2026-01-02T00:00:00Z", "2800"));
        reader.add("u002", point("u002", "daily_calories", "2026-01-01T00:00:00Z", "2000"));
        reader.add("u002", point("u002", "daily_calories", "2026-01-02T00:00:00Z", "2200"));
        reader.add("u001", point("u001", "weekly_activity_count", "2026-01-05T00:00:00Z", "5"));
        reader.add("u002", point("u002", "weekly_activity_count", "2026-01-05T00:00:00Z", "3"));
        reader.add("u001", point("u001", "daily_sleep_duration", "2026-01-01T00:00:00Z", "5.5"));
        reader.add("u002", point("u002", "daily_sleep_duration", "2026-01-01T00:00:00Z", "7"));
        reader.add("u001", point(
                "u001", "heart_rate_zone_minutes", "2026-01-01T00:00:00Z", "40", "cardio"));
        reader.add("u001", point(
                "u001", "heart_rate_zone_minutes", "2026-01-01T00:00:00Z", "10", "peak"));
        reader.add("u002", point(
                "u002", "heart_rate_zone_minutes", "2026-01-01T00:00:00Z", "20", "cardio"));

        DatasetRequestValidator validator = new DatasetRequestValidator(reader);
        service = new DatasetQueryService(reader, validator, aggregationService());
    }

    @Test
    void findsSubjectsWithAverageDailyCaloriesGreaterThan2500() {
        DatasetQueryResult result = service.query(query(
                "daily_calories", "gt", "2500", null, "avg", "2026-01-01", "2026-01-02", "json"));

        assertEquals(1, result.response().subjects().size());
        assertEquals("u001", result.response().subjects().get(0).userId());
        assertDecimal("2600", result.response().subjects().get(0).aggregatedValue());
    }

    @Test
    void findsSubjectsWithMoreThanFourWeeklyActivities() {
        DatasetQueryResult result = service.query(query(
                "weekly_activity_count", "gt", "4", null, "latest", null, null, "json"));

        assertEquals(List.of("u001"), result.response().subjects().stream().map(match -> match.userId()).toList());
    }

    @Test
    void findsSubjectsWithLessThanSixHoursOfSleep() {
        DatasetQueryResult result = service.query(query(
                "daily_sleep_duration", "lt", "6", null, "avg", null, null, "json"));

        assertEquals(List.of("u001"), result.response().subjects().stream().map(match -> match.userId()).toList());
    }

    @Test
    void supportsInclusiveBetween() {
        DatasetQueryResult result = service.query(query(
                "daily_calories", "between", "2500", "2700", "avg", null, null, "json"));

        assertEquals(List.of("u001"), result.response().subjects().stream().map(match -> match.userId()).toList());
    }

    @Test
    void appliesDateRangeBeforeAggregation() {
        DatasetQueryResult result = service.query(query(
                "daily_calories", "eq", "2400", null, "avg", "2026-01-01", "2026-01-01", "json"));

        assertEquals(List.of("u001"), result.response().subjects().stream().map(match -> match.userId()).toList());
    }

    @Test
    void filtersMetricDimensionsBeforeAggregation() {
        DatasetQueryRequest request = new DatasetQueryRequest(
                "heart_rate_zone_minutes",
                "gt",
                BigDecimal.valueOf(30),
                null,
                "sum",
                Map.of("zone", List.of("CARDIO")),
                null,
                null,
                "json"
        );

        DatasetQueryResult result = service.query(request);

        assertEquals(List.of("u001"), result.response().subjects().stream().map(match -> match.userId()).toList());
        assertDecimal("40", result.response().subjects().get(0).aggregatedValue());
        assertEquals(List.of("cardio"), result.response().query().dimensions().get("zone"));
    }

    @Test
    void rejectsUnknownMetric() {
        assertThrows(UnsupportedMetricException.class, () -> service.query(query(
                "unknown_metric", "gt", "1", null, "avg", null, null, "json")));
    }

    @Test
    void rejectsUnsupportedFormat() {
        assertThrows(UnsupportedDatasetFormatException.class, () -> service.query(query(
                "daily_calories", "gt", "1", null, "avg", null, null, "xml")));
    }

    private DatasetAggregationService aggregationService() {
        return new DatasetAggregationService(List.of(
                new AverageAggregationStrategy(),
                new SumAggregationStrategy(),
                new MinAggregationStrategy(),
                new MaxAggregationStrategy(),
                new CountAggregationStrategy(),
                new LatestAggregationStrategy()
        ));
    }

    private DatasetQueryRequest query(String metric,
                                      String operator,
                                      String value,
                                      String maxValue,
                                      String aggregation,
                                      String from,
                                      String to,
                                      String format) {
        return new DatasetQueryRequest(
                metric,
                operator,
                new BigDecimal(value),
                maxValue == null ? null : new BigDecimal(maxValue),
                aggregation,
                null,
                from == null ? null : LocalDate.parse(from),
                to == null ? null : LocalDate.parse(to),
                format
        );
    }

    private TimeSeriesPoint point(String userId, String metric, String periodStart, String value) {
        return point(userId, metric, periodStart, value, null);
    }

    private TimeSeriesPoint point(String userId,
                                  String metric,
                                  String periodStart,
                                  String value,
                                  String zone) {
        Instant start = Instant.parse(periodStart);
        return new TimeSeriesPoint(
                userId,
                metric,
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
                zone,
                start
        );
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static final class InMemoryReader implements LongitudinalDatasetReader {

        private final List<SubjectInfo> subjects = new ArrayList<>();
        private final Map<String, Map<String, List<TimeSeriesPoint>>> points = new LinkedHashMap<>();

        void addSubject(String userId) {
            subjects.add(new SubjectInfo(userId, userId, Set.of(), "Z", null, null, null, null, null));
        }

        void add(String userId, TimeSeriesPoint point) {
            points.computeIfAbsent(userId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(point.metricName(), ignored -> new ArrayList<>())
                    .add(point);
        }

        @Override
        public List<SubjectInfo> subjects() {
            return List.copyOf(subjects);
        }

        @Override
        public Set<String> availableMetrics() {
            return points.values().stream()
                    .flatMap(metrics -> metrics.keySet().stream())
                    .collect(java.util.stream.Collectors.toSet());
        }

        @Override
        public List<TimeSeriesPoint> readPoints(String userId, String metric, LocalDate from, LocalDate to) {
            return points.getOrDefault(userId, Map.of())
                    .getOrDefault(metric, List.of())
                    .stream()
                    .filter(point -> {
                        LocalDate date = point.periodStart().atZone(ZoneOffset.UTC).toLocalDate();
                        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
                    })
                    .toList();
        }
    }
}
