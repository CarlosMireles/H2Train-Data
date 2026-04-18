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
import com.h2traindata.infrastructure.provider.fitbit.client.FitbitApiClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FitbitUserMetricsCollectorTest {

    private final FitbitApiClient fitbitApiClient = Mockito.mock(FitbitApiClient.class);
    private final FitbitUserMetricsCollector collector = new FitbitUserMetricsCollector(
            new FitbitUserMetricsPayloadFetcher(fitbitApiClient, command -> command.run()),
            new FitbitUserMetricsEventFactory()
    );

    @Test
    void mapsProfileAndLifetimeStatsToUserMetricsSnapshot() {
        ProviderConnection connection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("ABC123", "fitbit-runner"),
                "fitbit-access-token",
                "fitbit-refresh-token",
                Instant.now().plusSeconds(600)
        );

        when(fitbitApiClient.fetchProfilePayload("fitbit-access-token")).thenReturn(Map.of(
                "user", Map.ofEntries(
                        Map.entry("encodedId", "ABC123"),
                        Map.entry("displayName", "fitbit-runner"),
                        Map.entry("fullName", "Fitbit Runner"),
                        Map.entry("firstName", "Fitbit"),
                        Map.entry("lastName", "Runner"),
                        Map.entry("avatar640", "https://example.com/avatar640.png"),
                        Map.entry("city", "Madrid"),
                        Map.entry("country", "Spain"),
                        Map.entry("gender", "MALE"),
                        Map.entry("timezone", "Europe/Madrid"),
                        Map.entry("offsetFromUTCMillis", 7200000L),
                        Map.entry("distanceUnit", "METRIC"),
                        Map.entry("weight", 72.5),
                        Map.entry("weightUnit", "KG"),
                        Map.entry("height", 178.0),
                        Map.entry("heightUnit", "CM"),
                        Map.entry("averageDailySteps", 10432)
                )
        ));
        when(fitbitApiClient.fetchLifetimeStats("fitbit-access-token")).thenReturn(Map.of(
                "lifetime", Map.of(
                        "total", Map.of(
                                "distance", 8302.5,
                                "steps", 11152512,
                                "floors", 10278
                        )
                ),
                "best", Map.of(
                        "total", Map.of(
                                "distance", Map.of("value", 28.69259, "date", "2016-07-17")
                        )
                )
        ));
        when(fitbitApiClient.fetchActivityGoals(eq("fitbit-access-token"), eq("daily"))).thenReturn(Map.of(
                "goals", Map.of(
                        "activeMinutes", 55,
                        "activeZoneMinutes", 21,
                        "caloriesOut", 3500,
                        "distance", 5.0,
                        "floors", 10,
                        "steps", 10000
                )
        ));
        when(fitbitApiClient.fetchActivityGoals(eq("fitbit-access-token"), eq("weekly"))).thenReturn(Map.of(
                "goals", Map.of(
                        "activeZoneMinutes", 150,
                        "distance", 56.33,
                        "floors", 70,
                        "steps", 70000
                )
        ));
        when(fitbitApiClient.fetchDailyActivitySummary(eq("fitbit-access-token"), any())).thenReturn(Map.of(
                "summary", Map.ofEntries(
                        Map.entry("activityCalories", 525),
                        Map.entry("caloriesBMR", 1973),
                        Map.entry("caloriesOut", 2628),
                        Map.entry("distances", List.of(
                                Map.of("activity", "total", "distance", 1.26),
                                Map.of("activity", "tracker", "distance", 1.26)
                        )),
                        Map.entry("floors", 0),
                        Map.entry("lightlyActiveMinutes", 110),
                        Map.entry("fairlyActiveMinutes", 0),
                        Map.entry("veryActiveMinutes", 0),
                        Map.entry("sedentaryMinutes", 802),
                        Map.entry("restingHeartRate", 77),
                        Map.entry("heartRateZones", List.of(
                                Map.of("name", "Fat Burn", "minutes", 619)
                        )),
                        Map.entry("steps", 1698)
                )
        ));
        when(fitbitApiClient.fetchAzmTimeSeries(eq("fitbit-access-token"), any(), any())).thenReturn(Map.of(
                "activities-active-zone-minutes", List.of(
                        Map.of(
                                "dateTime", "2026-04-09",
                                "value", Map.of(
                                        "activeZoneMinutes", 25,
                                        "fatBurnActiveZoneMinutes", 7,
                                        "cardioActiveZoneMinutes", 18,
                                        "peakActiveZoneMinutes", 0
                                )
                        )
                )
        ));
        when(fitbitApiClient.fetchActivityTimeSeries(eq("fitbit-access-token"), eq("steps"), any(), any())).thenReturn(Map.of(
                "activities-steps", List.of(Map.of("dateTime", "2026-04-09", "value", "10432"))
        ));
        when(fitbitApiClient.fetchActivityTimeSeries(eq("fitbit-access-token"), eq("distance"), any(), any())).thenReturn(Map.of(
                "activities-distance", List.of(Map.of("dateTime", "2026-04-09", "value", "8.3"))
        ));
        when(fitbitApiClient.fetchActivityTimeSeries(eq("fitbit-access-token"), eq("calories"), any(), any())).thenReturn(Map.of(
                "activities-calories", List.of(Map.of("dateTime", "2026-04-09", "value", "2628"))
        ));
        when(fitbitApiClient.fetchWeightLog(eq("fitbit-access-token"), any(), any())).thenReturn(Map.of(
                "weight", List.of(
                        Map.of(
                                "bmi", 22.9,
                                "date", "2026-04-09",
                                "fat", 18.7,
                                "logId", 1551080804000L,
                                "source", "Aria",
                                "time", "07:46:44",
                                "weight", 72.2
                        )
                )
        ));
        when(fitbitApiClient.fetchBodyFatLog(eq("fitbit-access-token"), any(), any())).thenReturn(Map.of(
                "fat", List.of(
                        Map.of(
                                "date", "2026-04-09",
                                "fat", 18.7,
                                "logId", 1532187349000L,
                                "source", "Aria",
                                "time", "07:46:44"
                        )
                )
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
        when(fitbitApiClient.fetchDevices("fitbit-access-token")).thenReturn(List.of(
                Map.of(
                        "battery", "High",
                        "batteryLevel", 95,
                        "deviceVersion", "Charge 6",
                        "id", "816713257",
                        "lastSyncTime", "2026-04-10T09:05:58.000",
                        "type", "TRACKER"
                ),
                Map.of(
                        "battery", "Medium",
                        "batteryLevel", 60,
                        "deviceVersion", "Aria Air",
                        "id", "scale-1",
                        "lastSyncTime", "2026-04-09T07:46:44.000",
                        "type", "SCALE"
                )
        ));
        when(fitbitApiClient.fetchFriends("fitbit-access-token")).thenReturn(Map.of(
                "data", List.of(
                        Map.of(
                                "id", "friend-1",
                                "attributes", Map.of(
                                        "avatar", "https://example.com/friend-1.png",
                                        "child", false,
                                        "friend", true,
                                        "name", "Ana"
                                )
                        ),
                        Map.of(
                                "id", "friend-2",
                                "attributes", Map.of(
                                        "avatar", "https://example.com/friend-2.png",
                                        "child", false,
                                        "friend", true,
                                        "name", "Luis"
                                )
                        )
                )
        ));
        when(fitbitApiClient.fetchFriendsLeaderboard("fitbit-access-token")).thenReturn(Map.of(
                "data", List.of(
                        Map.of(
                                "id", "ABC123",
                                "attributes", Map.of(
                                        "step-rank", 1,
                                        "step-summary", 65432.0
                                )
                        )
                )
        ));
        when(fitbitApiClient.fetchHeartRateTimeSeries(eq("fitbit-access-token"), any(), any())).thenReturn(Map.of(
                "activities-heart", List.of(
                        Map.of(
                                "dateTime", "2026-04-09",
                                "value", Map.of(
                                        "restingHeartRate", 58,
                                        "heartRateZones", List.of(
                                                Map.of("name", "Fat Burn", "minutes", 40)
                                        ),
                                        "customHeartRateZones", List.of(
                                                Map.of("name", "Custom Zone", "minutes", 12)
                                        )
                                )
                        )
                )
        ));
        when(fitbitApiClient.fetchNutritionTimeSeries(eq("fitbit-access-token"), eq("caloriesIn"), any(), any())).thenReturn(Map.of(
                "foods-log-caloriesIn", List.of(Map.of("dateTime", "2026-04-09", "value", "2150"))
        ));
        when(fitbitApiClient.fetchNutritionTimeSeries(eq("fitbit-access-token"), eq("water"), any(), any())).thenReturn(Map.of(
                "foods-log-water", List.of(Map.of("dateTime", "2026-04-09", "value", "2200"))
        ));
        when(fitbitApiClient.fetchActivityIntraday(eq("fitbit-access-token"), eq("steps"), any())).thenReturn(Map.of(
                "activities-steps", List.of(Map.of("dateTime", "2026-04-10", "value", "9123")),
                "activities-steps-intraday", Map.of(
                        "dataset", List.of(
                                Map.of("time", "06:00:00", "value", 100),
                                Map.of("time", "06:01:00", "value", 120)
                        ),
                        "datasetInterval", 1,
                        "datasetType", "minute"
                )
        ));
        when(fitbitApiClient.fetchHeartRateIntraday(eq("fitbit-access-token"), any())).thenReturn(Map.of(
                "activities-heart", List.of(Map.of("dateTime", "2026-04-10")),
                "activities-heart-intraday", Map.of(
                        "dataset", List.of(
                                Map.of("time", "06:00:00", "value", 61),
                                Map.of("time", "06:01:00", "value", 63)
                        ),
                        "datasetInterval", 1,
                        "datasetType", "minute"
                )
        ));
        when(fitbitApiClient.fetchBreathingRateSummary(eq("fitbit-access-token"), any(), any())).thenReturn(List.of(
                Map.of(
                        "dateTime", "2026-04-09",
                        "value", Map.of(
                                "breathingRate", 15.4,
                                "deepSleepBreathingRate", 14.1,
                                "fullSleepBreathingRate", 15.4
                        )
                )
        ));
        when(fitbitApiClient.fetchVo2MaxSummary(eq("fitbit-access-token"), any(), any())).thenReturn(List.of(
                Map.of(
                        "dateTime", "2026-04-09",
                        "value", Map.of(
                                "vo2Max", 48.6,
                                "fitnessLevel", "excellent"
                        )
                )
        ));
        when(fitbitApiClient.fetchBloodGlucose(eq("fitbit-access-token"), any(), any())).thenReturn(List.of(
                Map.of(
                        "timestamp", "2026-04-09T08:15:00",
                        "value", 103,
                        "unit", "mg/dL",
                        "mealContext", "fasting"
                )
        ));
        when(fitbitApiClient.fetchEcgLogList(eq("fitbit-access-token"), anyString(), any(), anyInt())).thenReturn(Map.of(
                "ecgLogs", List.of(
                        Map.of(
                                "startTime", "2026-04-09T07:30:00",
                                "resultClassification", "sinus_rhythm",
                                "averageHeartRate", 66,
                                "symptoms", List.of("fatigue")
                        )
                )
        ));
        when(fitbitApiClient.fetchSleepLogList(eq("fitbit-access-token"), anyString(), any(), anyInt())).thenReturn(Map.of(
                "sleep", List.of(
                        Map.ofEntries(
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
                        )
                )
        ));
        when(fitbitApiClient.fetchSpo2Summary(eq("fitbit-access-token"), any(), any())).thenReturn(List.of(
                Map.of(
                        "dateTime", "2026-04-09",
                        "value", Map.of(
                                "avg", 97.5,
                                "min", 95.1,
                                "max", 99.0
                        )
                )
        ));
        when(fitbitApiClient.fetchHrvSummary(eq("fitbit-access-token"), any(), any())).thenReturn(List.of(
                Map.of(
                        "dateTime", "2026-04-09",
                        "value", Map.of(
                                "dailyRmssd", 42.3,
                                "deepRmssd", 55.2,
                                "coverage", 0.94
                        )
                )
        ));
        when(fitbitApiClient.fetchTemperatureSkinSummary(eq("fitbit-access-token"), any(), any())).thenReturn(List.of(
                Map.of(
                        "dateTime", "2026-04-09",
                        "value", Map.of(
                                "temperature", 33.2,
                                "nightlyRelative", -0.3
                        )
                )
        ));
        when(fitbitApiClient.fetchTemperatureCoreSummary(eq("fitbit-access-token"), any(), any())).thenReturn(List.of(
                Map.of(
                        "dateTime", "2026-04-09",
                        "value", Map.of("coreTemperature", 36.7)
                )
        ));
        when(fitbitApiClient.fetchIrnProfile("fitbit-access-token")).thenReturn(Map.of(
                "profile", Map.of("enabled", true)
        ));
        when(fitbitApiClient.fetchIrnAlerts(eq("fitbit-access-token"), anyString(), any(), anyInt())).thenReturn(Map.of(
                "alerts", List.of(
                        Map.of(
                                "alertedAt", "2026-04-10T06:15:00",
                                "result", "possible_afib",
                                "status", "new"
                        )
                )
        ));

        EventBatch batch = collector.collect(connection, null);
        Map<String, Object> normalized = batch.events().get(0).normalizedPayload();
        Map<String, Object> latestSleep = castMap(normalized.get("latestSleep"));
        Map<String, Object> latestBreathingRate = castMap(normalized.get("latestBreathingRate"));
        Map<String, Object> latestVo2Max = castMap(normalized.get("latestVo2Max"));
        Map<String, Object> latestBloodGlucose = castMap(normalized.get("latestBloodGlucose"));
        Map<String, Object> latestElectrocardiogram = castMap(normalized.get("latestElectrocardiogram"));
        Map<String, Object> latestSpo2 = castMap(normalized.get("latestBloodOxygenSaturation"));
        Map<String, Object> latestHrv = castMap(normalized.get("latestHeartRateVariability"));
        Map<String, Object> latestSkinTemperature = castMap(normalized.get("latestSkinTemperature"));
        Map<String, Object> latestIrn = castMap(normalized.get("latestIrregularRhythmNotification"));
        Map<String, Object> activityGoalsDaily = castMap(normalized.get("activityGoalsDaily"));
        Map<String, Object> latestActivitySummary = castMap(normalized.get("latestActivitySummary"));
        Map<String, Object> latestAzm = castMap(normalized.get("latestActiveZoneMinutes"));
        Map<String, Object> latestBodyComposition = castMap(normalized.get("latestBodyComposition"));
        Map<String, Object> latestHeartRateSummary = castMap(normalized.get("latestHeartRateSummary"));
        Map<String, Object> latestNutrition = castMap(normalized.get("latestNutrition"));
        Map<String, Object> latestIntradaySteps = castMap(normalized.get("latestIntradayStepsSample"));
        Map<String, Object> latestIntradayHeartRate = castMap(normalized.get("latestIntradayHeartRateSample"));

        assertEquals(EventType.USER_METRICS, batch.eventType());
        assertEquals(1, batch.events().size());
        assertEquals("ABC123", normalized.get("providerAthleteId"));
        assertEquals("fitbit-runner", normalized.get("displayName"));
        assertEquals("male", normalized.get("gender"));
        assertEquals("metric", normalized.get("measurementSystem"));
        assertEquals(8302500.0, normalized.get("lifetimeDistanceMeters"));
        assertEquals(11152512L, normalized.get("lifetimeSteps"));
        assertEquals(15.4, latestBreathingRate.get("breathsPerMinute"));
        assertEquals(48.6, latestVo2Max.get("score"));
        assertEquals("mg/dL", latestBloodGlucose.get("unit"));
        assertEquals("sinus_rhythm", latestElectrocardiogram.get("resultClassification"));
        assertEquals(97.5, latestSpo2.get("averagePct"));
        assertEquals(42.3, latestHrv.get("dailyRmssd"));
        assertEquals(33.2, latestSkinTemperature.get("temperatureC"));
        assertEquals(Boolean.TRUE, normalized.get("irregularRhythmNotificationsEnabled"));
        assertEquals("possible_afib", latestIrn.get("result"));
        assertEquals("12345", latestSleep.get("logId"));
        assertEquals(28800, latestSleep.get("durationSeconds"));
        assertEquals(Instant.parse("2026-04-08T21:00:00Z"), latestSleep.get("startTime"));
        assertEquals(10000, activityGoalsDaily.get("steps"));
        assertEquals(1698, latestActivitySummary.get("steps"));
        assertEquals(25, latestAzm.get("activeZoneMinutes"));
        assertEquals(72.2, latestBodyComposition.get("weightKg"));
        assertEquals(18.7, latestBodyComposition.get("bodyFatPct"));
        assertEquals(2, normalized.get("deviceCount"));
        assertEquals(2, normalized.get("friendCount"));
        assertEquals(1, normalized.get("friendsLeaderboardRank"));
        assertEquals(65432.0, normalized.get("friendsLeaderboardStepSummary"));
        assertEquals(58.0, normalized.get("restingHeartRateBpm"));
        assertEquals(58.0, latestHeartRateSummary.get("restingHeartRateBpm"));
        assertEquals(2150.0, latestNutrition.get("caloriesInKcal"));
        assertEquals(2200.0, latestNutrition.get("water"));
        assertEquals(120.0, latestIntradaySteps.get("value"));
        assertEquals(63.0, latestIntradayHeartRate.get("value"));
        assertTrue(batch.events().get(0).eventId().startsWith("ABC123:"));
        assertEquals("METRIC", batch.events().get(0).providerSpecificPayload().get("distanceUnit"));
        assertTrue(batch.events().get(0).providerSpecificPayload().containsKey("devices"));
        assertTrue(batch.events().get(0).providerSpecificPayload().containsKey("nutritionTimeSeries"));
        assertTrue(batch.events().get(0).providerSpecificPayload().containsKey("spo2"));
        assertTrue(batch.events().get(0).rawPayload().containsKey("electrocardiogram"));
        assertTrue(batch.events().get(0).rawPayload().containsKey("activityIntradaySteps"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
