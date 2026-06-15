package com.h2traindata.dataapp.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h2traindata.dataapp.config.DataAppDatalakeProperties;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import com.h2traindata.dataapp.infrastructure.LongitudinalDatamartRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TimeSeriesProjectionServiceTest {

    private static final String USER_ID = "user-1";

    @TempDir
    private Path tempDir;

    private TimeSeriesProjectionService projectionService;
    private TimeSeriesQueryService queryService;
    private LongitudinalDatamartRepository datamartRepository;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        DataAppDatalakeProperties properties = new DataAppDatalakeProperties();
        properties.setRootPath(tempDir.resolve("datalake"));
        datamartRepository = new LongitudinalDatamartRepository(properties, objectMapper);
        TimeSeriesBuilderService builderService = new TimeSeriesBuilderService(
                Clock.fixed(Instant.parse("2026-06-08T12:00:00Z"), ZoneOffset.UTC)
        );
        projectionService = new TimeSeriesProjectionService(builderService, datamartRepository, objectMapper);
        queryService = new TimeSeriesQueryService(datamartRepository);
    }

    @Test
    void apiReadsProjectedDatamartAndDuplicateEventsDoNotDuplicatePoints() {
        NormalizedDatalakeEvent steps = event("fitbit", "PHYSIOLOGICAL", "Steps", "steps-1",
                Instant.parse("2026-06-10T08:00:00Z"),
                node -> node.put("steps", 10500));

        projectionService.process(steps);
        projectionService.process(steps);

        List<TimeSeriesPoint> points = queryService.timeSeries(
                USER_ID,
                "daily_steps",
                LocalDate.parse("2026-06-10"),
                LocalDate.parse("2026-06-10")
        );
        assertEquals(1, points.size());
        assertEquals(0, points.get(0).value().compareTo(java.math.BigDecimal.valueOf(10500)));
        assertTrue(Files.exists(tempDir.resolve("datalake").resolve("datamarts").resolve("longitudinal")));
        assertFalse(Files.exists(tempDir.resolve("datalake").resolve("events")));
    }

    @Test
    void workoutUpdatesOnlyAffectedWeeklyAggregatesIncrementally() {
        projectionService.process(workout("workout-run-1", "run", "2026-06-10T08:00:00Z", 1800, 5000, 400));
        projectionService.process(workout("workout-run-2", "run", "2026-06-11T08:00:00Z", 1200, 3000, 250));

        TimeSeriesPoint weeklyCount = onlyPoint(queryService.weeklySummary(
                USER_ID,
                LocalDate.parse("2026-06-08"),
                LocalDate.parse("2026-06-14")
        ), "weekly_activity_count");
        TimeSeriesPoint weeklyDistance = onlyPoint(queryService.weeklySummary(
                USER_ID,
                LocalDate.parse("2026-06-08"),
                LocalDate.parse("2026-06-14")
        ), "weekly_workout_distance_by_sport");

        assertEquals(0, weeklyCount.value().compareTo(java.math.BigDecimal.valueOf(2)));
        assertEquals("run", weeklyDistance.activityType());
        assertEquals(0, weeklyDistance.value().compareTo(java.math.BigDecimal.valueOf(8000)));
    }

    @Test
    void primaryStepsDeactivateActivitySummaryFallbackForSameDay() {
        NormalizedDatalakeEvent summary = event("fitbit", "ACTIVITY", "ActivitySummary", "summary-1",
                Instant.parse("2026-06-10T00:00:00Z"),
                node -> {
                    node.put("steps", 500);
                    node.put("distanceMeters", 350.0);
                    node.put("caloriesOut", 1800.0);
                });
        NormalizedDatalakeEvent steps = event("fitbit", "PHYSIOLOGICAL", "Steps", "steps-1",
                Instant.parse("2026-06-10T00:00:00Z"),
                node -> node.put("steps", 600));

        projectionService.process(summary);
        assertEquals(0, queryService.timeSeries(USER_ID, "daily_steps", null, null)
                .get(0)
                .value()
                .compareTo(java.math.BigDecimal.valueOf(500)));

        projectionService.process(steps);

        List<TimeSeriesPoint> points = queryService.timeSeries(USER_ID, "daily_steps", null, null);
        assertEquals(1, points.size());
        assertEquals(0, points.get(0).value().compareTo(java.math.BigDecimal.valueOf(600)));
        assertEquals("Steps", points.get(0).sourceEventName());
    }

    @Test
    void nutritionAndBloodGlucoseUpdateLongitudinalMetricsIncrementally() {
        NormalizedDatalakeEvent nutrition = event(
                "fitbit",
                "BODY_COMPOSITION",
                "Nutrition",
                "nutrition-1",
                Instant.parse("2026-06-10T08:00:00Z"),
                node -> {
                    node.put("calories", 2100);
                    node.put("water", 1800);
                });
        NormalizedDatalakeEvent glucose = event(
                "fitbit",
                "HEALTH",
                "BloodGlucose",
                "glucose-1",
                Instant.parse("2026-06-10T09:00:00Z"),
                node -> node.put("averageGlucose", 98));

        projectionService.process(nutrition);
        projectionService.process(nutrition);
        projectionService.process(glucose);

        TimeSeriesPoint calories = onlyPoint(
                queryService.timeSeries(USER_ID, "daily_calories_ingested", null, null),
                "daily_calories_ingested");
        TimeSeriesPoint water = onlyPoint(
                queryService.timeSeries(USER_ID, "daily_water_consumed", null, null),
                "daily_water_consumed");
        TimeSeriesPoint bloodGlucose = onlyPoint(
                queryService.timeSeries(USER_ID, "daily_blood_glucose", null, null),
                "daily_blood_glucose");

        assertEquals(0, calories.value().compareTo(java.math.BigDecimal.valueOf(2100)));
        assertEquals(0, water.value().compareTo(java.math.BigDecimal.valueOf(1800)));
        assertEquals(0, bloodGlucose.value().compareTo(java.math.BigDecimal.valueOf(98)));
        assertEquals("kcal", calories.unit());
        assertEquals("ml", water.unit());
        assertEquals("mg/dL", bloodGlucose.unit());
    }

    @Test
    void workoutEventMaintainsActivityReadModelIncrementally() {
        projectionService.process(workout("workout-run-1", "run", "2026-06-10T08:00:00Z", 1800, 5000, 400));

        var page = queryService.activities(USER_ID, null, null, null, 0, 10);

        assertEquals(1, page.totalItems());
        assertEquals("workout-run-1", page.items().get(0).activityId());
        assertEquals("run", page.items().get(0).activityType());
    }

    @Test
    void syncEventMaintainsProviderStatusAndHistoryReadModels() {
        NormalizedDatalakeEvent sync = event("h2train", "ACCOUNT_SYNC", "provider_account_synced", "sync-1",
                Instant.parse("2026-06-10T08:00:00Z"),
                node -> {
                    node.put("linkedProviderId", "strava");
                    node.put("syncEnabled", true);
                    node.put("syncInterval", "PT24H");
                });

        projectionService.process(sync);

        assertTrue(queryService.user(USER_ID).providers().contains("strava"));
        assertEquals(1, queryService.syncStatus(USER_ID).size());
        assertEquals("strava", queryService.syncStatus(USER_ID).get(0).provider());
        assertEquals(1, queryService.syncHistory(USER_ID).size());
        assertEquals("sync-1", queryService.syncHistory(USER_ID).get(0).syncId());
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
                    node.put("duration", duration);
                    node.put("distanceMeters", distanceMeters);
                    node.put("calories", calories);
                });
    }

    private NormalizedDatalakeEvent event(String providerId,
                                          String eventType,
                                          String eventName,
                                          String eventId,
                                          Instant eventTimestamp,
                                          Consumer<ObjectNode> customizer) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("timestamp", eventTimestamp.toString());
        node.put("sourceSystem", providerId);
        customizer.accept(node);
        return new NormalizedDatalakeEvent(
                USER_ID,
                providerId,
                "athlete-1",
                eventType,
                eventName,
                eventId,
                eventTimestamp,
                eventTimestamp,
                node
        );
    }

    private TimeSeriesPoint onlyPoint(List<TimeSeriesPoint> points, String metricName) {
        List<TimeSeriesPoint> matches = points.stream()
                .filter(point -> metricName.equals(point.metricName()))
                .toList();
        assertEquals(1, matches.size(), "Expected one point for " + metricName);
        return matches.get(0);
    }
}
