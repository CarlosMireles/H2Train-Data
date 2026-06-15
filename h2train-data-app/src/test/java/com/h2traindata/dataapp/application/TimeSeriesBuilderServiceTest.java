package com.h2traindata.dataapp.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.h2traindata.dataapp.domain.AggregationType;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import com.h2traindata.dataapp.domain.TimeSeriesDataset;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class TimeSeriesBuilderServiceTest {

    private static final String USER_ID = "user-1";
    private static final Instant GENERATED_AT = Instant.parse("2026-06-08T12:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TimeSeriesBuilderService service = new TimeSeriesBuilderService(
            Clock.fixed(GENERATED_AT, ZoneOffset.UTC)
    );

    @Test
    void transformsStepsToDailySteps() {
        NormalizedDatalakeEvent steps = event("fitbit", "PHYSIOLOGICAL", "Steps", "steps-1",
                Instant.parse("2026-04-10T09:30:00Z"),
                node -> node.put("steps", 12345));

        TimeSeriesDataset dataset = service.buildDataset(List.of(steps));

        TimeSeriesPoint point = onlyPoint(dataset, "daily_steps");
        assertPointValue("12345", point);
        assertEquals("P1D", point.period());
        assertEquals(Instant.parse("2026-04-10T00:00:00Z"), point.periodStart());
        assertEquals(Instant.parse("2026-04-11T00:00:00Z"), point.periodEnd());
        assertEquals("steps", point.unit());
        assertEquals(AggregationType.SUM, point.aggregationType());
    }

    @Test
    void transformsSleepToDailySleepDurationUsingStartTime() {
        NormalizedDatalakeEvent sleep = event("fitbit", "HEALTH", "Sleep", "sleep-1",
                Instant.parse("2026-04-09T05:00:00Z"),
                node -> {
                    node.put("startTime", "2026-04-08T22:30:00Z");
                    node.put("endTime", "2026-04-09T06:30:00Z");
                    node.put("duration", 28800);
                });

        TimeSeriesDataset dataset = service.buildDataset(List.of(sleep));

        TimeSeriesPoint point = onlyPoint(dataset, "daily_sleep_duration");
        assertPointValue("28800", point);
        assertEquals(Instant.parse("2026-04-08T00:00:00Z"), point.periodStart());
        assertEquals("s", point.unit());
        assertEquals(AggregationType.SUM, point.aggregationType());
    }

    @Test
    void transformsNutritionToDailyCaloriesIngestedAndWaterConsumed() {
        NormalizedDatalakeEvent breakfast = event("fitbit", "BODY_COMPOSITION", "Nutrition", "nutrition-1",
                Instant.parse("2026-04-10T08:00:00Z"),
                node -> {
                    node.put("calories", 650);
                    node.put("water", 500);
                });
        NormalizedDatalakeEvent dinner = event("fitbit", "BODY_COMPOSITION", "Nutrition", "nutrition-2",
                Instant.parse("2026-04-10T20:00:00Z"),
                node -> {
                    node.put("calories", 1200);
                    node.put("water", 1000);
                });

        TimeSeriesDataset dataset = service.buildDataset(List.of(breakfast, dinner));

        TimeSeriesPoint calories = onlyPoint(dataset, "daily_calories_ingested");
        TimeSeriesPoint water = onlyPoint(dataset, "daily_water_consumed");
        assertPointValue("1850", calories);
        assertPointValue("1500", water);
        assertEquals("kcal", calories.unit());
        assertEquals("ml", water.unit());
        assertEquals(AggregationType.SUM, calories.aggregationType());
        assertEquals(AggregationType.SUM, water.aggregationType());
    }

    @Test
    void transformsBloodGlucoseToLatestDailyValue() {
        NormalizedDatalakeEvent morning = event("fitbit", "HEALTH", "BloodGlucose", "glucose-1",
                Instant.parse("2026-04-10T08:00:00Z"),
                node -> node.put("averageGlucose", 92));
        NormalizedDatalakeEvent evening = event("fitbit", "HEALTH", "BloodGlucose", "glucose-2",
                Instant.parse("2026-04-10T20:00:00Z"),
                node -> node.put("averageGlucose", 105));

        TimeSeriesDataset dataset = service.buildDataset(List.of(morning, evening));

        TimeSeriesPoint glucose = onlyPoint(dataset, "daily_blood_glucose");
        assertPointValue("105", glucose);
        assertEquals("mg/dL", glucose.unit());
        assertEquals(AggregationType.LAST, glucose.aggregationType());
    }

    @Test
    void transformsWorkoutToDailyAggregates() {
        NormalizedDatalakeEvent run = workout("workout-run", "run", "2026-04-08T10:00:00Z", 1800, 5000, 450);
        NormalizedDatalakeEvent walk = workout("workout-walk", "walk", "2026-04-08T18:00:00Z", 1200, 1000, 100);

        TimeSeriesDataset dataset = service.buildDataset(List.of(run, walk));

        assertPointValue("2", onlyPoint(dataset, "daily_activity_count"));
        assertPointValue("3000", onlyPoint(dataset, "daily_workout_duration"));
        assertPointValue("6000", onlyPoint(dataset, "daily_workout_distance"));
        assertPointValue("550", onlyPoint(dataset, "daily_workout_calories"));
    }

    @Test
    void transformsWorkoutToWeeklyAggregatesByActivityType() {
        NormalizedDatalakeEvent run = workout("workout-run", "run", "2026-04-08T10:00:00Z", 1800, 5000, 450);
        NormalizedDatalakeEvent walk = workout("workout-walk", "walk", "2026-04-09T18:00:00Z", 1200, 1000, 100);

        TimeSeriesDataset dataset = service.buildDataset(List.of(run, walk));

        TimeSeriesPoint runDistance = point(dataset, "weekly_workout_distance_by_sport", "run", null);
        TimeSeriesPoint walkDistance = point(dataset, "weekly_workout_distance_by_sport", "walk", null);
        assertPointValue("5000", runDistance);
        assertPointValue("1000", walkDistance);
        assertEquals(Instant.parse("2026-04-06T00:00:00Z"), runDistance.periodStart());
        assertEquals(Instant.parse("2026-04-13T00:00:00Z"), runDistance.periodEnd());
    }

    @Test
    void transformsHeartRateZonesToZoneDimension() {
        NormalizedDatalakeEvent cardioA = heartRate("hr-cardio-a", "Cardio", 30, 200);
        NormalizedDatalakeEvent cardioB = heartRate("hr-cardio-b", "Cardio", 15, 75);
        NormalizedDatalakeEvent peak = heartRate("hr-peak", "Peak", 5, 50);

        TimeSeriesDataset dataset = service.buildDataset(List.of(cardioA, cardioB, peak));

        TimeSeriesPoint cardioMinutes = point(dataset, "heart_rate_zone_minutes", null, "cardio");
        TimeSeriesPoint cardioCalories = point(dataset, "heart_rate_zone_calories", null, "cardio");
        TimeSeriesPoint peakMinutes = point(dataset, "heart_rate_zone_minutes", null, "peak");
        assertPointValue("45", cardioMinutes);
        assertPointValue("275", cardioCalories);
        assertPointValue("5", peakMinutes);
    }

    @Test
    void synchronizationIntervalDoesNotChangeRealPeriodStartOrDuplicatePoints() {
        Instant eventTime = Instant.parse("2026-04-10T09:30:00Z");
        NormalizedDatalakeEvent firstSync = event("fitbit", "PHYSIOLOGICAL", "Steps", "steps-same-event",
                eventTime,
                Instant.parse("2026-04-11T09:30:00Z"),
                node -> node.put("steps", 1000));
        NormalizedDatalakeEvent secondSync = event("fitbit", "PHYSIOLOGICAL", "Steps", "steps-same-event",
                eventTime,
                Instant.parse("2026-04-12T09:30:00Z"),
                node -> node.put("steps", 1000));

        TimeSeriesDataset dataset = service.buildDataset(List.of(firstSync, secondSync));

        TimeSeriesPoint point = onlyPoint(dataset, "daily_steps");
        assertPointValue("1000", point);
        assertEquals(Instant.parse("2026-04-10T00:00:00Z"), point.periodStart());
        assertEquals(GENERATED_AT, point.generatedAt());
    }

    private NormalizedDatalakeEvent workout(String eventId,
                                            String activityType,
                                            String startTime,
                                            int duration,
                                            double distanceMeters,
                                            double calories) {
        return event("strava", "ACTIVITY", "Workout", eventId, Instant.parse(startTime),
                node -> {
                    node.put("activityType", activityType);
                    node.put("startTime", startTime);
                    node.put("endTime", Instant.parse(startTime).plusSeconds(duration).toString());
                    node.put("duration", duration);
                    node.put("distanceMeters", distanceMeters);
                    node.put("calories", calories);
                });
    }

    private NormalizedDatalakeEvent heartRate(String eventId, String zone, int minutes, double calories) {
        return event("fitbit", "PHYSIOLOGICAL", "HeartRate", eventId,
                Instant.parse("2026-04-10T00:00:00Z"),
                node -> {
                    node.put("zone", zone);
                    node.put("minutes", minutes);
                    node.put("calories", calories);
                });
    }

    private NormalizedDatalakeEvent event(String providerId,
                                          String eventType,
                                          String eventName,
                                          String eventId,
                                          Instant eventTimestamp,
                                          Consumer<ObjectNode> customizer) {
        return event(providerId, eventType, eventName, eventId, eventTimestamp, eventTimestamp, customizer);
    }

    private NormalizedDatalakeEvent event(String providerId,
                                          String eventType,
                                          String eventName,
                                          String eventId,
                                          Instant eventTimestamp,
                                          Instant publishedAt,
                                          Consumer<ObjectNode> customizer) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("timestamp", eventTimestamp.toString());
        node.put("sourceSystem", providerId);
        customizer.accept(node);
        JsonNode event = node;
        return new NormalizedDatalakeEvent(
                USER_ID,
                providerId,
                "athlete-1",
                eventType,
                eventName,
                eventId,
                eventTimestamp,
                publishedAt,
                event
        );
    }

    private TimeSeriesPoint onlyPoint(TimeSeriesDataset dataset, String metricName) {
        List<TimeSeriesPoint> points = dataset.points().stream()
                .filter(point -> metricName.equals(point.metricName()))
                .toList();
        assertEquals(1, points.size(), "Expected exactly one point for " + metricName);
        return points.get(0);
    }

    private TimeSeriesPoint point(TimeSeriesDataset dataset, String metricName, String activityType, String zone) {
        TimeSeriesPoint point = dataset.points().stream()
                .filter(candidate -> metricName.equals(candidate.metricName()))
                .filter(candidate -> activityType == null || activityType.equals(candidate.activityType()))
                .filter(candidate -> zone == null || zone.equals(candidate.zone()))
                .findFirst()
                .orElse(null);
        assertNotNull(point, "Expected point for " + metricName);
        return point;
    }

    private void assertPointValue(String expected, TimeSeriesPoint point) {
        assertEquals(0, new BigDecimal(expected).compareTo(point.value()));
    }
}
