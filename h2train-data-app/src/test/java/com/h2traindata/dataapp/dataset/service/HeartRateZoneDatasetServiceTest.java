package com.h2traindata.dataapp.dataset.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetRequest;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDay;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneValue;
import com.h2traindata.dataapp.dataset.exception.UnsupportedMetricException;
import com.h2traindata.dataapp.domain.AggregationType;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HeartRateZoneDatasetServiceTest {

    @Test
    void buildsUsefulDailyViewWithoutMixingProviders() {
        InMemoryReader reader = new InMemoryReader(true);
        reader.add(point("heart_rate_zone_minutes", "fitbit", "cardio", "30"));
        reader.add(point("heart_rate_zone_minutes", "fitbit", "peak", "10"));
        reader.add(point("heart_rate_zone_minutes", "fitbit", "out_of_range", "1400"));
        reader.add(point("heart_rate_zone_calories", "fitbit", "cardio", "200"));
        reader.add(point("heart_rate_zone_calories", "fitbit", "peak", "100"));
        reader.add(point("heart_rate_zone_calories", "fitbit", "out_of_range", "1200"));
        reader.add(point("heart_rate_zone_minutes", "other", "cardio", "20"));

        HeartRateZoneDatasetResult result = new HeartRateZoneDatasetService(reader).query(
                new HeartRateZoneDatasetRequest(
                        LocalDate.parse("2026-01-02"),
                        LocalDate.parse("2026-01-02"),
                        null,
                        null,
                        "json"
                )
        );

        assertEquals(2, result.response().days().size());
        HeartRateZoneDay fitbit = result.response().days().get(0);
        assertEquals("fitbit", fitbit.provider());
        assertDecimal("1440", fitbit.trackedMinutes());
        assertDecimal("40", fitbit.activeMinutes());
        assertDecimal("1500", fitbit.totalCalories());
        assertDecimal("300", fitbit.activeCalories());
        assertDecimal("40", fitbit.highIntensityMinutes());
        assertEquals("cardio", fitbit.dominantActiveZone());
        HeartRateZoneValue cardio = fitbit.zones().stream()
                .filter(zone -> "cardio".equals(zone.zone()))
                .findFirst()
                .orElseThrow();
        assertDecimal("2.08", cardio.percentageOfTrackedTime());
        assertDecimal("75", cardio.percentageOfActiveTime());
    }

    @Test
    void filtersReturnedZonesButKeepsWholeDaySummary() {
        InMemoryReader reader = new InMemoryReader(true);
        reader.add(point("heart_rate_zone_minutes", "fitbit", "cardio", "30"));
        reader.add(point("heart_rate_zone_minutes", "fitbit", "peak", "10"));
        reader.add(point("heart_rate_zone_minutes", "fitbit", "out_of_range", "1400"));

        HeartRateZoneDay day = new HeartRateZoneDatasetService(reader).query(
                new HeartRateZoneDatasetRequest(null, null, List.of("CARDIO"), List.of("FITBIT"), "json")
        ).response().days().get(0);

        assertEquals(List.of("cardio"), day.zones().stream().map(HeartRateZoneValue::zone).toList());
        assertDecimal("1440", day.trackedMinutes());
        assertDecimal("40", day.activeMinutes());
        assertEquals("cardio", day.dominantActiveZone());
    }

    @Test
    void leavesDominantActiveZoneEmptyWhenThereIsNoActiveTime() {
        InMemoryReader reader = new InMemoryReader(true);
        reader.add(point("heart_rate_zone_minutes", "fitbit", "out_of_range", "1440"));

        HeartRateZoneDay day = new HeartRateZoneDatasetService(reader).query(
                new HeartRateZoneDatasetRequest(null, null, null, null, "json")
        ).response().days().get(0);

        assertNull(day.dominantActiveZone());
        assertDecimal("0", day.activeMinutes());
    }

    @Test
    void rejectsDatamartWithoutHeartRateZoneMinutes() {
        InMemoryReader reader = new InMemoryReader(false);

        assertThrows(UnsupportedMetricException.class, () -> new HeartRateZoneDatasetService(reader).query(
                new HeartRateZoneDatasetRequest(null, null, null, null, "json")
        ));
    }

    private static TimeSeriesPoint point(String metric, String provider, String zone, String value) {
        Instant start = Instant.parse("2026-01-01T23:00:00Z");
        return new TimeSeriesPoint(
                "u001",
                metric,
                "P1D",
                start,
                start.plusSeconds(86400),
                new BigDecimal(value),
                metric.endsWith("minutes") ? "min" : "kcal",
                provider,
                "PHYSIOLOGICAL",
                "HeartRate",
                AggregationType.SUM,
                null,
                zone,
                start
        );
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static final class InMemoryReader implements LongitudinalDatasetReader {

        private final boolean exposeMetric;
        private final Map<String, List<TimeSeriesPoint>> points = new LinkedHashMap<>();

        private InMemoryReader(boolean exposeMetric) {
            this.exposeMetric = exposeMetric;
        }

        void add(TimeSeriesPoint point) {
            points.computeIfAbsent(point.metricName(), ignored -> new ArrayList<>()).add(point);
        }

        @Override
        public List<SubjectInfo> subjects() {
            return List.of(new SubjectInfo(
                    "u001",
                    "u001",
                    Set.of("fitbit", "other"),
                    "Europe/Madrid",
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        @Override
        public Set<String> availableMetrics() {
            return exposeMetric
                    ? Set.of("heart_rate_zone_minutes", "heart_rate_zone_calories")
                    : Set.of();
        }

        @Override
        public List<TimeSeriesPoint> readPoints(String userId, String metric, LocalDate from, LocalDate to) {
            return List.copyOf(points.getOrDefault(metric, List.of()));
        }
    }
}
