package com.h2traindata.infrastructure.provider.fitbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.infrastructure.provider.fitbit.client.FitbitApiClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FitbitUserMetricsCollectorTest {

    private final FitbitApiClient fitbitApiClient = Mockito.mock(FitbitApiClient.class);
    private final FitbitUserMetricsPayloadFetcher payloadFetcher =
            new FitbitUserMetricsPayloadFetcher(fitbitApiClient, command -> command.run());
    private final FitbitUserMetricsEventFactory eventFactory = new FitbitUserMetricsEventFactory();
    private final FitbitUserMetricsCollector userStateCollector =
            new FitbitUserMetricsCollector(payloadFetcher, eventFactory);
    private final FitbitPhysiologicalCollector physiologicalCollector =
            new FitbitPhysiologicalCollector(payloadFetcher, eventFactory);
    private final FitbitBodyCompositionCollector bodyCompositionCollector =
            new FitbitBodyCompositionCollector(payloadFetcher, eventFactory);
    private final FitbitHealthCollector healthCollector =
            new FitbitHealthCollector(payloadFetcher, eventFactory);

    @Test
    void mapsFitbitSnapshotsToOntologyBatches() {
        ProviderConnection connection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("ABC123", "fitbit-runner"),
                "fitbit-access-token",
                "fitbit-refresh-token",
                Instant.now().plusSeconds(600)
        );

        stubSnapshotApis();

        EventBatch userStateBatch = userStateCollector.collect(connection, null);
        EventBatch physiologicalBatch = physiologicalCollector.collect(connection, null);
        EventBatch bodyCompositionBatch = bodyCompositionCollector.collect(connection, null);
        EventBatch healthBatch = healthCollector.collect(connection, null);

        ProviderEvent userProfile = findEvent(userStateBatch, "UserProfile");
        ProviderEvent dailyGoals = findEvent(userStateBatch, "UserGoals", event ->
                "Daily".equals(event.field("scale")));
        ProviderEvent weeklyGoals = findEvent(userStateBatch, "UserGoals", event ->
                "Weekly".equals(event.field("scale")));

        assertEquals(EventType.USER_STATE, userStateBatch.eventType());
        assertEquals(3, userStateBatch.events().size());
        assertEquals("fitbit", userProfile.sourceSystem());
        assertEquals("ABC123", userProfile.athleteId());
        assertEquals(72.5, userProfile.field("weight"));
        assertEquals(178.0, userProfile.field("height"));
        assertEquals("male", userProfile.field("gender"));
        assertEquals("Europe/Madrid", userProfile.field("timezone"));
        assertEquals(java.util.Set.of("weight", "height", "gender", "timezone"), userProfile.fields().keySet());
        assertEquals(10000, dailyGoals.field("steps"));
        assertEquals(70000, weeklyGoals.field("steps"));

        ProviderEvent steps = findEvent(physiologicalBatch, "Steps");
        ProviderEvent distance = findEvent(physiologicalBatch, "Distance");
        ProviderEvent caloriesBurned = findEvent(physiologicalBatch, "CaloriesBurned");
        ProviderEvent heartRate = findEvent(physiologicalBatch, "HeartRate");

        assertEquals(EventType.PHYSIOLOGICAL, physiologicalBatch.eventType());
        assertEquals(1698, steps.field("steps"));
        assertEquals(1260.0, distance.field("distanceMeters"));
        assertEquals(2628.0, caloriesBurned.field("calories"));
        assertEquals("Fat Burn", heartRate.field("zone"));
        assertEquals(619, heartRate.field("minutes"));

        ProviderEvent bodyComposition = findEvent(bodyCompositionBatch, "BodyComposition");
        ProviderEvent nutrition = findEvent(bodyCompositionBatch, "Nutrition");

        assertEquals(EventType.BODY_COMPOSITION, bodyCompositionBatch.eventType());
        assertEquals(72.2, bodyComposition.field("weight"));
        assertEquals(22.9, bodyComposition.field("bodyMassIndex"));
        assertEquals(18.7, bodyComposition.field("bodyFatPercentage"));
        assertEquals(2150.0, nutrition.field("calories"));
        assertEquals(2200.0, nutrition.field("water"));

        ProviderEvent sleep = findEvent(healthBatch, "Sleep");
        ProviderEvent bloodGlucose = findEvent(healthBatch, "BloodGlucose");
        ProviderEvent electrocardiogram = findEvent(healthBatch, "Electrocardiogram");
        ProviderEvent anomaly = findEvent(healthBatch, "AnomalyDetected");
        Map<String, Object> electrocardiogramData = castMap(electrocardiogram.field("electrocardiogramData"));

        assertEquals(EventType.HEALTH, healthBatch.eventType());
        assertEquals(28800, sleep.field("duration"));
        assertEquals(Instant.parse("2026-04-08T21:00:00Z"), sleep.field("startTime"));
        assertEquals(104.0, bloodGlucose.field("averageGlucose"));
        assertEquals(Instant.parse("2026-04-09T05:30:00Z"), electrocardiogram.field("measurementTimestamp"));
        assertEquals("sinus_rhythm", electrocardiogramData.get("resultClassification"));
        assertEquals(66.0, electrocardiogramData.get("heartRateBeatsPerMinute"));
        assertEquals(Instant.parse("2026-04-10T04:15:00Z"), anomaly.field("detectedTimestamp"));
        assertTrue(String.valueOf(anomaly.field("description")).contains("possible_afib"));
    }

    private void stubSnapshotApis() {
        when(fitbitApiClient.fetchProfilePayload("fitbit-access-token")).thenReturn(Map.of(
                "user", Map.ofEntries(
                        Map.entry("encodedId", "ABC123"),
                        Map.entry("displayName", "fitbit-runner"),
                        Map.entry("fullName", "Fitbit Runner"),
                        Map.entry("gender", "MALE"),
                        Map.entry("timezone", "Europe/Madrid"),
                        Map.entry("distanceUnit", "METRIC"),
                        Map.entry("weight", 72.5),
                        Map.entry("weightUnit", "KG"),
                        Map.entry("height", 178.0),
                        Map.entry("heightUnit", "CM")
                )
        ));
        when(fitbitApiClient.fetchActivityGoals(eq("fitbit-access-token"), eq("daily"))).thenReturn(Map.of(
                "goals", Map.of(
                        "activeMinutes", 55,
                        "caloriesOut", 3500,
                        "distance", 5.0,
                        "steps", 10000
                )
        ));
        when(fitbitApiClient.fetchActivityGoals(eq("fitbit-access-token"), eq("weekly"))).thenReturn(Map.of(
                "goals", Map.of(
                        "distance", 56.33,
                        "steps", 70000
                )
        ));
        when(fitbitApiClient.fetchDailyActivitySummary(eq("fitbit-access-token"), any())).thenReturn(Map.of(
                "summary", Map.ofEntries(
                        Map.entry("activityCalories", 525),
                        Map.entry("caloriesOut", 2628),
                        Map.entry("distances", List.of(
                                Map.of("activity", "total", "distance", 1.26),
                                Map.of("activity", "tracker", "distance", 1.26)
                        )),
                        Map.entry("lightlyActiveMinutes", 110),
                        Map.entry("fairlyActiveMinutes", 0),
                        Map.entry("veryActiveMinutes", 0),
                        Map.entry("sedentaryMinutes", 802),
                        Map.entry("restingHeartRate", 77),
                        Map.entry("heartRateZones", List.of(
                                Map.of("name", "Fat Burn", "min", 91, "max", 127, "minutes", 619, "caloriesOut", 400.0)
                        )),
                        Map.entry("steps", 1698)
                )
        ));
        when(fitbitApiClient.fetchWeightLog(eq("fitbit-access-token"), any(), any())).thenReturn(Map.of(
                "weight", List.of(Map.of(
                        "bmi", 22.9,
                        "date", "2026-04-09",
                        "fat", 18.7,
                        "source", "Aria",
                        "time", "07:46:44",
                        "weight", 72.2
                ))
        ));
        when(fitbitApiClient.fetchBodyFatLog(eq("fitbit-access-token"), any(), any())).thenReturn(Map.of(
                "fat", List.of(Map.of(
                        "date", "2026-04-09",
                        "fat", 18.7,
                        "source", "Aria",
                        "time", "07:46:44"
                ))
        ));
        when(fitbitApiClient.fetchBodyTimeSeries(eq("fitbit-access-token"), eq("weight"), any(), any())).thenReturn(Map.of(
                "body-weight", List.of(Map.of("dateTime", "2026-04-09", "value", "72.2"))
        ));
        when(fitbitApiClient.fetchBodyTimeSeries(eq("fitbit-access-token"), eq("fat"), any(), any())).thenReturn(Map.of(
                "body-fat", List.of(Map.of("dateTime", "2026-04-09", "value", "18.7"))
        ));
        when(fitbitApiClient.fetchBodyTimeSeries(eq("fitbit-access-token"), eq("bmi"), any(), any())).thenReturn(Map.of(
                "body-bmi", List.of(Map.of("dateTime", "2026-04-09", "value", "22.9"))
        ));
        when(fitbitApiClient.fetchNutritionTimeSeries(eq("fitbit-access-token"), eq("caloriesIn"), any(), any())).thenReturn(Map.of(
                "foods-log-caloriesIn", List.of(Map.of("dateTime", "2026-04-09", "value", "2150"))
        ));
        when(fitbitApiClient.fetchNutritionTimeSeries(eq("fitbit-access-token"), eq("water"), any(), any())).thenReturn(Map.of(
                "foods-log-water", List.of(Map.of("dateTime", "2026-04-09", "value", "2200"))
        ));
        when(fitbitApiClient.fetchBloodGlucose(eq("fitbit-access-token"), any(), any())).thenReturn(List.of(
                Map.of(
                        "dateTime", "2026-04-09",
                        "value", Map.of(
                                "glucose", 104.0,
                                "unit", "mg/dL"
                        )
                )
        ));
        when(fitbitApiClient.fetchEcgLogList(eq("fitbit-access-token"), anyString(), any(), anyInt())).thenReturn(Map.of(
                "ecgLogs", List.of(Map.of(
                        "startTime", "2026-04-09T07:30:00",
                        "resultClassification", "sinus_rhythm",
                        "averageHeartRate", 66,
                        "symptoms", List.of("fatigue")
                ))
        ));
        when(fitbitApiClient.fetchSleepLogList(eq("fitbit-access-token"), anyString(), any(), anyInt())).thenReturn(Map.of(
                "sleep", List.of(Map.ofEntries(
                        Map.entry("logId", 12345),
                        Map.entry("startTime", "2026-04-08T23:00:00"),
                        Map.entry("endTime", "2026-04-09T07:00:00"),
                        Map.entry("dateOfSleep", "2026-04-09"),
                        Map.entry("duration", 28800000L),
                        Map.entry("timeInBed", 480),
                        Map.entry("minutesAsleep", 430),
                        Map.entry("minutesAwake", 30),
                        Map.entry("minutesAfterWakeup", 10),
                        Map.entry("minutesToFallAsleep", 10),
                        Map.entry("efficiency", 95),
                        Map.entry("mainSleep", true),
                        Map.entry("type", "stages"),
                        Map.entry("score", Map.of("overall", 86)),
                        Map.entry("levels", Map.of(
                                "summary", Map.of(
                                        "deep", Map.of("count", 2, "minutes", 80),
                                        "light", Map.of("count", 3, "minutes", 230),
                                        "rem", Map.of("count", 2, "minutes", 120),
                                        "wake", Map.of("count", 4, "minutes", 50)
                                )
                        ))
                ))
        ));
        when(fitbitApiClient.fetchIrnProfile("fitbit-access-token")).thenReturn(Map.of(
                "profile", Map.of("enabled", true)
        ));
        when(fitbitApiClient.fetchIrnAlerts(eq("fitbit-access-token"), anyString(), any(), anyInt())).thenReturn(Map.of(
                "alerts", List.of(Map.of(
                        "alertedAt", "2026-04-10T06:15:00",
                        "result", "possible_afib",
                        "status", "new"
                ))
        ));
    }

    private ProviderEvent findEvent(EventBatch batch, String eventName) {
        return findEvent(batch, eventName, event -> true);
    }

    private ProviderEvent findEvent(EventBatch batch, String eventName, Predicate<ProviderEvent> predicate) {
        return batch.events().stream()
                .filter(event -> eventName.equals(event.eventName()))
                .filter(predicate)
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
