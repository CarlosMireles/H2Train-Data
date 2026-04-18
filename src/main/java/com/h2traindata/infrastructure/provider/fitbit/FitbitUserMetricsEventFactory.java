package com.h2traindata.infrastructure.provider.fitbit;

import static com.h2traindata.infrastructure.provider.common.PayloadSupport.booleanValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.doubleValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.instantValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.intValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.listOfMaps;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.listValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.longValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.mapValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.put;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.stringValue;

import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderEvent;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "app.fitbit", name = "enabled", havingValue = "true")
class FitbitUserMetricsEventFactory {

    ProviderEvent createEvent(String providerId,
                              String connectionAthleteId,
                              EventType eventType,
                              Instant snapshotAt,
                              FitbitUserMetricsPayloadBundle payloadBundle) {
        return new ProviderEvent(
                providerId,
                connectionAthleteId,
                eventType,
                snapshotEventId(payloadBundle.athleteId(), snapshotAt),
                snapshotAt,
                normalizedPayload(
                        payloadBundle.user(),
                        payloadBundle.lifetimeStats(),
                        payloadBundle.healthPayloads(),
                        payloadBundle.supplementalPayloads(),
                        payloadBundle.endDate()
                ),
                providerSpecificPayload(
                        payloadBundle.user(),
                        payloadBundle.lifetimeStats(),
                        payloadBundle.healthPayloads(),
                        payloadBundle.supplementalPayloads(),
                        payloadBundle.startDate(),
                        payloadBundle.endDate()
                ),
                rawPayload(
                        payloadBundle.profilePayload(),
                        payloadBundle.lifetimeStats(),
                        payloadBundle.healthPayloads(),
                        payloadBundle.supplementalPayloads()
                )
        );
    }

    private Map<String, Object> normalizedPayload(Map<String, Object> user,
                                                  Map<String, Object> lifetimeStats,
                                                  FitbitHealthPayloads healthPayloads,
                                                  FitbitSupplementalPayloads supplementalPayloads,
                                                  LocalDate snapshotDate) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> lifetime = mapValue(lifetimeStats.get("lifetime"));
        Map<String, Object> totalLifetime = mapValue(lifetime.get("total"));
        String distanceUnit = stringValue(user.get("distanceUnit"));
        String timezone = stringValue(user.get("timezone"));
        String athleteId = stringValue(user.get("encodedId"));
        List<Map<String, Object>> devices = supplementalPayloads.devices();
        Map<String, Object> latestHeartRateSummary = normalizeHeartRateSummary(
                supplementalPayloads.heartRateTimeSeries(),
                supplementalPayloads.dailyActivitySummary(),
                timezone,
                snapshotDate
        );
        Map<String, Object> latestBodyComposition = normalizeBodyComposition(
                supplementalPayloads.weightLog(),
                supplementalPayloads.bodyFatLog(),
                supplementalPayloads.bodyWeightTimeSeries(),
                supplementalPayloads.bodyFatTimeSeries(),
                supplementalPayloads.bodyBmiTimeSeries(),
                timezone,
                stringValue(user.get("weightUnit"))
        );

        put(normalized, "providerAthleteId", stringValue(user.get("encodedId")));
        put(normalized, "username", firstNonBlank(stringValue(user.get("displayName")), stringValue(user.get("fullName"))));
        put(normalized, "displayName", stringValue(user.get("displayName")));
        put(normalized, "fullName", stringValue(user.get("fullName")));
        put(normalized, "firstName", stringValue(user.get("firstName")));
        put(normalized, "lastName", stringValue(user.get("lastName")));
        put(normalized, "avatarUrl", preferredAvatar(user));
        put(normalized, "city", stringValue(user.get("city")));
        put(normalized, "state", stringValue(user.get("state")));
        put(normalized, "country", stringValue(user.get("country")));
        put(normalized, "gender", normalizeGender(stringValue(user.get("gender"))));
        put(normalized, "timezone", stringValue(user.get("timezone")));
        put(normalized, "timezoneOffsetSeconds", timezoneOffsetSeconds(user.get("offsetFromUTCMillis")));
        put(normalized, "measurementSystem", measurementSystem(distanceUnit));
        put(normalized, "weightKg", weightKg(user));
        put(normalized, "heightCm", heightCm(user));
        put(normalized, "averageDailySteps", intValue(user.get("averageDailySteps")));
        put(normalized, "lifetimeDistanceMeters", convertDistanceToMeters(doubleValue(totalLifetime.get("distance")), distanceUnit));
        put(normalized, "lifetimeSteps", longValue(totalLifetime.get("steps")));
        put(normalized, "lifetimeFloors", longValue(totalLifetime.get("floors")));
        put(normalized, "activityGoalsDaily", normalizeActivityGoals(supplementalPayloads.activityGoalsDaily(), distanceUnit));
        put(normalized, "activityGoalsWeekly", normalizeActivityGoals(supplementalPayloads.activityGoalsWeekly(), distanceUnit));
        put(normalized, "latestActivitySummary", normalizeActivitySummary(
                supplementalPayloads.dailyActivitySummary(),
                snapshotDate,
                distanceUnit,
                timezone
        ));
        put(normalized, "latestActiveZoneMinutes", normalizeActiveZoneMinutes(
                latestEntry(entries(supplementalPayloads.azmTimeSeries(), "activities-active-zone-minutes"), timezone, "dateTime"),
                timezone
        ));
        put(normalized, "latestBodyComposition", latestBodyComposition);
        put(normalized, "deviceCount", devices.size());
        put(normalized, "lastDeviceSyncAt", latestDeviceSyncAt(devices, timezone));
        put(normalized, "friendCount", entries(supplementalPayloads.friends(), "data").size());
        put(normalized, "friendsLeaderboardRank", intValue(selfLeaderboardEntry(supplementalPayloads.friendsLeaderboard(), athleteId).get("step-rank")));
        put(normalized, "friendsLeaderboardStepSummary", doubleValue(selfLeaderboardEntry(
                supplementalPayloads.friendsLeaderboard(),
                athleteId
        ).get("step-summary")));
        put(normalized, "restingHeartRateBpm", doubleValue(latestHeartRateSummary.get("restingHeartRateBpm")));
        put(normalized, "latestHeartRateSummary", latestHeartRateSummary);
        put(normalized, "latestNutrition", normalizeNutrition(
                supplementalPayloads.nutritionCalories(),
                supplementalPayloads.nutritionWater(),
                measurementSystem(distanceUnit),
                timezone
        ));
        put(normalized, "latestIntradayStepsSample", normalizeIntradaySample(supplementalPayloads.activityIntradaySteps(), timezone));
        put(normalized, "latestIntradayHeartRateSample", normalizeIntradaySample(supplementalPayloads.heartRateIntraday(), timezone));
        put(normalized, "latestBreathingRate", normalizeBreathingRate(latestEntry(healthPayloads.breathingRate(), timezone, "dateTime"), timezone));
        put(normalized, "latestVo2Max", normalizeVo2Max(latestEntry(healthPayloads.vo2Max(), timezone, "dateTime"), timezone));
        put(normalized, "latestBloodGlucose", normalizeBloodGlucose(latestEntry(healthPayloads.bloodGlucose(), timezone, "timestamp", "dateTime", "recordedTime"), timezone));
        put(normalized, "latestElectrocardiogram", normalizeElectrocardiogram(latestEntry(
                entries(healthPayloads.ecg(), "ecgLogs", "ecgReadings", "ecg", "items"),
                timezone,
                "startTime",
                "dateTime",
                "timestamp"
        ), timezone));
        put(normalized, "latestSleep", normalizeSleep(
                latestEntry(entries(healthPayloads.sleep(), "sleep", "sleepLogs"), timezone, "startTime", "dateOfSleep"),
                timezone
        ));
        put(normalized, "latestBloodOxygenSaturation", normalizeBloodOxygenSaturation(latestEntry(healthPayloads.spo2(), timezone, "dateTime"), timezone));
        put(normalized, "latestHeartRateVariability", normalizeHeartRateVariability(latestEntry(healthPayloads.hrv(), timezone, "dateTime"), timezone));
        put(normalized, "latestSkinTemperature", normalizeTemperature(latestEntry(healthPayloads.temperatureSkin(), timezone, "dateTime"), timezone, "temperature"));
        put(normalized, "latestCoreTemperature", normalizeTemperature(latestEntry(healthPayloads.temperatureCore(), timezone, "dateTime"), timezone, "coreTemperature"));
        put(normalized, "irregularRhythmNotificationsEnabled", booleanValue(irnProfileValue(healthPayloads.irnProfile(), "enabled", "isEnabled")));
        put(normalized, "latestIrregularRhythmNotification", normalizeIrregularRhythmNotification(latestEntry(
                entries(healthPayloads.irnAlerts(), "alerts", "irnAlerts", "items"),
                timezone,
                "alertedAt",
                "startTime",
                "dateTime",
                "timestamp"
        ), timezone));
        return normalized;
    }

    private Map<String, Object> providerSpecificPayload(Map<String, Object> user,
                                                        Map<String, Object> lifetimeStats,
                                                        FitbitHealthPayloads healthPayloads,
                                                        FitbitSupplementalPayloads supplementalPayloads,
                                                        LocalDate startDate,
                                                        LocalDate endDate) {
        Map<String, Object> providerSpecific = new LinkedHashMap<>();
        put(providerSpecific, "aboutMe", stringValue(user.get("aboutMe")));
        put(providerSpecific, "age", intValue(user.get("age")));
        put(providerSpecific, "dateOfBirth", stringValue(user.get("dateOfBirth")));
        put(providerSpecific, "memberSince", stringValue(user.get("memberSince")));
        put(providerSpecific, "distanceUnit", stringValue(user.get("distanceUnit")));
        put(providerSpecific, "heightUnit", stringValue(user.get("heightUnit")));
        put(providerSpecific, "weightUnit", stringValue(user.get("weightUnit")));
        put(providerSpecific, "languageLocale", stringValue(user.get("languageLocale")));
        put(providerSpecific, "locale", stringValue(user.get("locale")));
        put(providerSpecific, "foodsLocale", stringValue(user.get("foodsLocale")));
        put(providerSpecific, "clockTimeDisplayFormat", stringValue(user.get("clockTimeDisplayFormat")));
        put(providerSpecific, "sleepTracking", stringValue(user.get("sleepTracking")));
        put(providerSpecific, "startDayOfWeek", stringValue(user.get("startDayOfWeek")));
        put(providerSpecific, "autoStrideEnabled", booleanValue(user.get("autoStrideEnabled")));
        put(providerSpecific, "strideLengthWalking", doubleValue(user.get("strideLengthWalking")));
        put(providerSpecific, "strideLengthWalkingType", stringValue(user.get("strideLengthWalkingType")));
        put(providerSpecific, "strideLengthRunning", doubleValue(user.get("strideLengthRunning")));
        put(providerSpecific, "strideLengthRunningType", stringValue(user.get("strideLengthRunningType")));
        put(providerSpecific, "topBadges", listOfMaps(user.get("topBadges")));
        put(providerSpecific, "features", mapValue(user.get("features")));
        put(providerSpecific, "lifetimeStats", lifetimeStats);
        put(providerSpecific, "healthMetricsWindowStart", startDate.toString());
        put(providerSpecific, "healthMetricsWindowEnd", endDate.toString());
        put(providerSpecific, "activityGoalsDaily", supplementalPayloads.activityGoalsDaily());
        put(providerSpecific, "activityGoalsWeekly", supplementalPayloads.activityGoalsWeekly());
        put(providerSpecific, "dailyActivitySummary", supplementalPayloads.dailyActivitySummary());
        put(providerSpecific, "activityTimeSeries", activityTimeSeriesPayload(supplementalPayloads));
        put(providerSpecific, "activeZoneMinutesTimeSeries", supplementalPayloads.azmTimeSeries());
        put(providerSpecific, "bodyWeightTimeSeries", supplementalPayloads.bodyWeightTimeSeries());
        put(providerSpecific, "bodyFatTimeSeries", supplementalPayloads.bodyFatTimeSeries());
        put(providerSpecific, "bodyBmiTimeSeries", supplementalPayloads.bodyBmiTimeSeries());
        put(providerSpecific, "weightLog", supplementalPayloads.weightLog());
        put(providerSpecific, "bodyFatLog", supplementalPayloads.bodyFatLog());
        put(providerSpecific, "devices", supplementalPayloads.devices());
        put(providerSpecific, "friends", supplementalPayloads.friends());
        put(providerSpecific, "friendsLeaderboard", supplementalPayloads.friendsLeaderboard());
        put(providerSpecific, "heartRateTimeSeries", supplementalPayloads.heartRateTimeSeries());
        put(providerSpecific, "nutritionTimeSeries", nutritionPayload(supplementalPayloads));
        put(providerSpecific, "intraday", intradayPayload(supplementalPayloads));
        put(providerSpecific, "breathingRate", healthPayloads.breathingRate());
        put(providerSpecific, "vo2Max", healthPayloads.vo2Max());
        put(providerSpecific, "bloodGlucose", healthPayloads.bloodGlucose());
        put(providerSpecific, "electrocardiogram", healthPayloads.ecg());
        put(providerSpecific, "sleep", healthPayloads.sleep());
        put(providerSpecific, "spo2", healthPayloads.spo2());
        put(providerSpecific, "hrv", healthPayloads.hrv());
        put(providerSpecific, "temperatureSkin", healthPayloads.temperatureSkin());
        put(providerSpecific, "temperatureCore", healthPayloads.temperatureCore());
        put(providerSpecific, "irnProfile", healthPayloads.irnProfile());
        put(providerSpecific, "irnAlerts", healthPayloads.irnAlerts());
        return providerSpecific;
    }

    private Map<String, Object> rawPayload(Map<String, Object> profilePayload,
                                           Map<String, Object> lifetimeStats,
                                           FitbitHealthPayloads healthPayloads,
                                           FitbitSupplementalPayloads supplementalPayloads) {
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        put(rawPayload, "profile", profilePayload);
        put(rawPayload, "lifetimeStats", lifetimeStats);
        put(rawPayload, "activityGoalsDaily", supplementalPayloads.activityGoalsDaily());
        put(rawPayload, "activityGoalsWeekly", supplementalPayloads.activityGoalsWeekly());
        put(rawPayload, "dailyActivitySummary", supplementalPayloads.dailyActivitySummary());
        put(rawPayload, "activeZoneMinutesTimeSeries", supplementalPayloads.azmTimeSeries());
        put(rawPayload, "activityTimeSeriesSteps", supplementalPayloads.activityTimeSeriesSteps());
        put(rawPayload, "activityTimeSeriesDistance", supplementalPayloads.activityTimeSeriesDistance());
        put(rawPayload, "activityTimeSeriesCalories", supplementalPayloads.activityTimeSeriesCalories());
        put(rawPayload, "weightLog", supplementalPayloads.weightLog());
        put(rawPayload, "bodyFatLog", supplementalPayloads.bodyFatLog());
        put(rawPayload, "bodyWeightTimeSeries", supplementalPayloads.bodyWeightTimeSeries());
        put(rawPayload, "bodyFatTimeSeries", supplementalPayloads.bodyFatTimeSeries());
        put(rawPayload, "bodyBmiTimeSeries", supplementalPayloads.bodyBmiTimeSeries());
        put(rawPayload, "devices", supplementalPayloads.devices());
        put(rawPayload, "friends", supplementalPayloads.friends());
        put(rawPayload, "friendsLeaderboard", supplementalPayloads.friendsLeaderboard());
        put(rawPayload, "heartRateTimeSeries", supplementalPayloads.heartRateTimeSeries());
        put(rawPayload, "nutritionCalories", supplementalPayloads.nutritionCalories());
        put(rawPayload, "nutritionWater", supplementalPayloads.nutritionWater());
        put(rawPayload, "activityIntradaySteps", supplementalPayloads.activityIntradaySteps());
        put(rawPayload, "heartRateIntraday", supplementalPayloads.heartRateIntraday());
        put(rawPayload, "breathingRate", healthPayloads.breathingRate());
        put(rawPayload, "vo2Max", healthPayloads.vo2Max());
        put(rawPayload, "bloodGlucose", healthPayloads.bloodGlucose());
        put(rawPayload, "electrocardiogram", healthPayloads.ecg());
        put(rawPayload, "sleep", healthPayloads.sleep());
        put(rawPayload, "spo2", healthPayloads.spo2());
        put(rawPayload, "hrv", healthPayloads.hrv());
        put(rawPayload, "temperatureSkin", healthPayloads.temperatureSkin());
        put(rawPayload, "temperatureCore", healthPayloads.temperatureCore());
        put(rawPayload, "irnProfile", healthPayloads.irnProfile());
        put(rawPayload, "irnAlerts", healthPayloads.irnAlerts());
        return rawPayload;
    }

    private String snapshotEventId(String athleteId, Instant snapshotAt) {
        return athleteId + ":" + snapshotAt.toEpochMilli();
    }

    private Map<String, Object> normalizeActivityGoals(Map<String, Object> payload, String distanceUnit) {
        Map<String, Object> goals = mapValue(payload.get("goals"));
        Map<String, Object> normalized = new LinkedHashMap<>();
        put(normalized, "activeMinutes", intValue(goals.get("activeMinutes")));
        put(normalized, "activeZoneMinutes", intValue(goals.get("activeZoneMinutes")));
        put(normalized, "caloriesOutKcal", doubleValue(goals.get("caloriesOut")));
        put(normalized, "distanceMeters", convertDistanceToMeters(doubleValue(goals.get("distance")), distanceUnit));
        put(normalized, "floors", intValue(goals.get("floors")));
        put(normalized, "steps", intValue(goals.get("steps")));
        return normalized;
    }

    private Map<String, Object> normalizeActivitySummary(Map<String, Object> payload,
                                                         LocalDate snapshotDate,
                                                         String distanceUnit,
                                                         String timezone) {
        Map<String, Object> summary = mapValue(payload.get("summary"));
        Map<String, Object> normalized = new LinkedHashMap<>();
        put(normalized, "recordedAt", snapshotDate.atStartOfDay(resolveZoneId(timezone)).toInstant());
        put(normalized, "date", snapshotDate.toString());
        put(normalized, "steps", intValue(summary.get("steps")));
        put(normalized, "activityCaloriesKcal", doubleValue(summary.get("activityCalories")));
        put(normalized, "caloriesBmrKcal", doubleValue(summary.get("caloriesBMR")));
        put(normalized, "caloriesOutKcal", doubleValue(summary.get("caloriesOut")));
        put(normalized, "distanceMeters", convertDistanceToMeters(distanceForActivity(summary, "total"), distanceUnit));
        put(normalized, "trackerDistanceMeters", convertDistanceToMeters(distanceForActivity(summary, "tracker"), distanceUnit));
        put(normalized, "floors", intValue(summary.get("floors")));
        put(normalized, "lightlyActiveMinutes", intValue(summary.get("lightlyActiveMinutes")));
        put(normalized, "fairlyActiveMinutes", intValue(summary.get("fairlyActiveMinutes")));
        put(normalized, "veryActiveMinutes", intValue(summary.get("veryActiveMinutes")));
        put(normalized, "sedentaryMinutes", intValue(summary.get("sedentaryMinutes")));
        put(normalized, "restingHeartRateBpm", doubleValue(summary.get("restingHeartRate")));
        put(normalized, "heartRateZones", listOfMaps(summary.get("heartRateZones")));
        return normalized;
    }

    private Map<String, Object> normalizeActiveZoneMinutes(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        put(normalized, "recordedAt", metricInstant(entry, timezone, "dateTime"));
        put(normalized, "activeZoneMinutes", intValue(value.get("activeZoneMinutes")));
        put(normalized, "fatBurnActiveZoneMinutes", intValue(value.get("fatBurnActiveZoneMinutes")));
        put(normalized, "cardioActiveZoneMinutes", intValue(value.get("cardioActiveZoneMinutes")));
        put(normalized, "peakActiveZoneMinutes", intValue(value.get("peakActiveZoneMinutes")));
        return normalized;
    }

    private Map<String, Object> normalizeBodyComposition(Map<String, Object> weightLogPayload,
                                                         Map<String, Object> bodyFatLogPayload,
                                                         Map<String, Object> bodyWeightTimeSeries,
                                                         Map<String, Object> bodyFatTimeSeries,
                                                         Map<String, Object> bodyBmiTimeSeries,
                                                         String timezone,
                                                         String weightUnit) {
        Map<String, Object> latestWeightLog = latestLogEntry(weightLogPayload, "weight", timezone);
        Map<String, Object> latestBodyFatLog = latestLogEntry(bodyFatLogPayload, "fat", timezone);
        Map<String, Object> latestWeightSeries = latestEntry(entries(bodyWeightTimeSeries, "body-weight"), timezone, "dateTime");
        Map<String, Object> latestBodyFatSeries = latestEntry(entries(bodyFatTimeSeries, "body-fat"), timezone, "dateTime");
        Map<String, Object> latestBmiSeries = latestEntry(entries(bodyBmiTimeSeries, "body-bmi"), timezone, "dateTime");
        Map<String, Object> normalized = new LinkedHashMap<>();

        put(normalized, "recordedAt", firstInstant(
                logInstant(latestWeightLog, timezone),
                logInstant(latestBodyFatLog, timezone),
                metricInstant(latestWeightSeries, timezone, "dateTime"),
                metricInstant(latestBodyFatSeries, timezone, "dateTime"),
                metricInstant(latestBmiSeries, timezone, "dateTime")
        ));
        put(normalized, "weightKg", convertWeightToKg(
                firstDouble(latestWeightLog.get("weight"), latestWeightSeries.get("value")),
                weightUnit
        ));
        put(normalized, "bmi", firstDouble(latestWeightLog.get("bmi"), latestBmiSeries.get("value")));
        put(normalized, "bodyFatPct", firstDouble(
                latestBodyFatLog.get("fat"),
                latestWeightLog.get("fat"),
                latestBodyFatSeries.get("value")
        ));
        put(normalized, "source", firstNonBlank(
                stringValue(latestWeightLog.get("source")),
                stringValue(latestBodyFatLog.get("source"))
        ));
        return normalized;
    }

    private Map<String, Object> normalizeHeartRateSummary(Map<String, Object> heartRateTimeSeriesPayload,
                                                          Map<String, Object> dailyActivitySummaryPayload,
                                                          String timezone,
                                                          LocalDate snapshotDate) {
        Map<String, Object> entry = latestEntry(entries(heartRateTimeSeriesPayload, "activities-heart"), timezone, "dateTime");
        if (entry.isEmpty()) {
            entry = dailyActivityHeartRateEntry(dailyActivitySummaryPayload, snapshotDate);
        }
        Map<String, Object> value = metricValue(entry);
        Map<String, Object> normalized = new LinkedHashMap<>();
        put(normalized, "recordedAt", metricInstant(entry, timezone, "dateTime"));
        put(normalized, "restingHeartRateBpm", doubleValue(value.get("restingHeartRate")));
        put(normalized, "heartRateZones", listOfMaps(value.get("heartRateZones")));
        put(normalized, "customHeartRateZones", listOfMaps(value.get("customHeartRateZones")));
        return normalized;
    }

    private Map<String, Object> normalizeNutrition(Map<String, Object> caloriesPayload,
                                                   Map<String, Object> waterPayload,
                                                   String measurementSystem,
                                                   String timezone) {
        Map<String, Object> caloriesEntry = latestEntry(entries(caloriesPayload, "foods-log-caloriesIn"), timezone, "dateTime");
        Map<String, Object> waterEntry = latestEntry(entries(waterPayload, "foods-log-water"), timezone, "dateTime");
        Map<String, Object> normalized = new LinkedHashMap<>();
        put(normalized, "recordedAt", firstInstant(
                metricInstant(caloriesEntry, timezone, "dateTime"),
                metricInstant(waterEntry, timezone, "dateTime")
        ));
        put(normalized, "caloriesInKcal", doubleValue(caloriesEntry.get("value")));
        put(normalized, "water", doubleValue(waterEntry.get("value")));
        put(normalized, "measurementSystem", measurementSystem);
        return normalized;
    }

    private Map<String, Object> normalizeIntradaySample(Map<String, Object> payload, String timezone) {
        Map<String, Object> datasetContainer = intradayDataset(payload);
        List<Map<String, Object>> dataset = listOfMaps(datasetContainer.get("dataset"));
        Map<String, Object> latestDataset = dataset.isEmpty() ? Map.of() : dataset.get(dataset.size() - 1);
        Map<String, Object> normalized = new LinkedHashMap<>();
        String date = intradayDate(payload);
        String time = stringValue(latestDataset.get("time"));
        if (StringUtils.hasText(date) && StringUtils.hasText(time)) {
            put(normalized, "recordedAt", parseMetricInstant(date + "T" + time, timezone));
        }
        put(normalized, "value", firstDouble(latestDataset.get("value"), latestDataset.get("level"), latestDataset.get("mets")));
        put(normalized, "datasetInterval", intValue(datasetContainer.get("datasetInterval")));
        put(normalized, "datasetType", stringValue(datasetContainer.get("datasetType")));
        return normalized;
    }

    private Map<String, Object> dailyActivityHeartRateEntry(Map<String, Object> dailyActivitySummaryPayload, LocalDate snapshotDate) {
        Map<String, Object> summary = mapValue(dailyActivitySummaryPayload.get("summary"));
        Map<String, Object> value = new LinkedHashMap<>();
        put(value, "restingHeartRate", summary.get("restingHeartRate"));
        put(value, "heartRateZones", summary.get("heartRateZones"));
        put(value, "customHeartRateZones", summary.get("customHeartRateZones"));
        Map<String, Object> entry = new LinkedHashMap<>();
        put(entry, "dateTime", snapshotDate.toString());
        put(entry, "value", value);
        return entry;
    }

    private Double distanceForActivity(Map<String, Object> summary, String activityName) {
        return listOfMaps(summary.get("distances")).stream()
                .filter(distance -> activityName.equalsIgnoreCase(stringValue(distance.get("activity"))))
                .map(distance -> doubleValue(distance.get("distance")))
                .filter(distance -> distance != null)
                .findFirst()
                .orElse(null);
    }

    private Instant latestDeviceSyncAt(List<Map<String, Object>> devices, String timezone) {
        return devices.stream()
                .map(device -> parseMetricInstant(device.get("lastSyncTime"), timezone))
                .filter(syncTime -> syncTime != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private Map<String, Object> selfLeaderboardEntry(Map<String, Object> leaderboardPayload, String athleteId) {
        Map<String, Object> entry = entries(leaderboardPayload, "data").stream()
                .filter(item -> athleteId != null && athleteId.equals(stringValue(item.get("id"))))
                .findFirst()
                .orElse(Map.of());
        return mapValue(entry.get("attributes"));
    }

    private Map<String, Object> activityTimeSeriesPayload(FitbitSupplementalPayloads supplementalPayloads) {
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "steps", supplementalPayloads.activityTimeSeriesSteps());
        put(payload, "distance", supplementalPayloads.activityTimeSeriesDistance());
        put(payload, "calories", supplementalPayloads.activityTimeSeriesCalories());
        return payload;
    }

    private Map<String, Object> nutritionPayload(FitbitSupplementalPayloads supplementalPayloads) {
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "caloriesIn", supplementalPayloads.nutritionCalories());
        put(payload, "water", supplementalPayloads.nutritionWater());
        return payload;
    }

    private Map<String, Object> intradayPayload(FitbitSupplementalPayloads supplementalPayloads) {
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "steps", supplementalPayloads.activityIntradaySteps());
        put(payload, "heartRate", supplementalPayloads.heartRateIntraday());
        return payload;
    }

    private Map<String, Object> latestLogEntry(Map<String, Object> payload, String key, String timezone) {
        List<Map<String, Object>> logs = listOfMaps(payload.get(key));
        Map<String, Object> latest = logs.stream()
                .filter(log -> logInstant(log, timezone) != null)
                .max(Comparator.comparing(log -> logInstant(log, timezone)))
                .orElse(Map.of());
        if (!latest.isEmpty()) {
            return latest;
        }
        return logs.isEmpty() ? Map.of() : logs.get(logs.size() - 1);
    }

    private Instant logInstant(Map<String, Object> entry, String timezone) {
        String date = stringValue(entry.get("date"));
        if (!StringUtils.hasText(date)) {
            return null;
        }
        String time = firstNonBlank(stringValue(entry.get("time")), "00:00:00");
        return parseMetricInstant(date + "T" + time, timezone);
    }

    private Instant firstInstant(Instant... values) {
        for (Instant value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String intradayDate(Map<String, Object> payload) {
        return payload.values().stream()
                .map(PayloadSupportEntry::maps)
                .filter(values -> !values.isEmpty())
                .map(values -> values.get(0))
                .map(value -> stringValue(value.get("dateTime")))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> intradayDataset(Map<String, Object> payload) {
        return payload.values().stream()
                .map(PayloadSupportEntry::map)
                .filter(value -> value.containsKey("dataset"))
                .findFirst()
                .orElse(Map.of());
    }

    private Map<String, Object> normalizeBreathingRate(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        Map<String, Object> fullSleepSummary = mapValue(value.get("fullSleepSummary"));
        Map<String, Object> deepSleepSummary = mapValue(value.get("deepSleepSummary"));
        put(normalized, "recordedAt", metricInstant(entry, timezone, "dateTime"));
        put(normalized, "breathsPerMinute", firstDouble(
                value.get("breathingRate"),
                fullSleepSummary.get("breathingRate"),
                entry.get("breathingRate"),
                entry.get("value")
        ));
        put(normalized, "deepSleepBreathsPerMinute", firstDouble(
                value.get("deepSleepBreathingRate"),
                deepSleepSummary.get("breathingRate")
        ));
        put(normalized, "fullSleepBreathsPerMinute", firstDouble(value.get("fullSleepBreathingRate"), fullSleepSummary.get("breathingRate")));
        return normalized;
    }

    private Map<String, Object> normalizeVo2Max(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        put(normalized, "recordedAt", metricInstant(entry, timezone, "dateTime"));
        put(normalized, "score", firstDouble(
                value.get("vo2Max"),
                value.get("cardioFitnessScore"),
                entry.get("vo2Max"),
                entry.get("cardioFitnessScore")
        ));
        put(normalized, "fitnessLevel", firstNonBlank(
                stringValue(value.get("fitnessLevel")),
                stringValue(value.get("level"))
        ));
        put(normalized, "classification", stringValue(value.get("classification")));
        return normalized;
    }

    private Map<String, Object> normalizeBloodGlucose(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        put(normalized, "recordedAt", metricInstant(entry, timezone, "timestamp", "dateTime", "recordedTime"));
        put(normalized, "value", firstDouble(
                value.get("glucose"),
                value.get("level"),
                value.get("value"),
                entry.get("glucose"),
                entry.get("value")
        ));
        put(normalized, "unit", firstNonBlank(stringValue(value.get("unit")), stringValue(entry.get("unit"))));
        put(normalized, "mealContext", firstNonBlank(
                stringValue(entry.get("mealContext")),
                stringValue(entry.get("mealType"))
        ));
        put(normalized, "outsideRange", firstBoolean(entry.get("outsideRange"), value.get("outsideRange")));
        put(normalized, "source", firstNonBlank(stringValue(entry.get("source")), stringValue(value.get("source"))));
        return normalized;
    }

    private Map<String, Object> normalizeElectrocardiogram(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        put(normalized, "recordedAt", metricInstant(entry, timezone, "startTime", "dateTime", "timestamp"));
        put(normalized, "durationSeconds", durationSeconds(firstLong(entry.get("duration"), value.get("duration"))));
        put(normalized, "resultClassification", firstNonBlank(
                stringValue(entry.get("resultClassification")),
                stringValue(value.get("resultClassification")),
                stringValue(entry.get("result")),
                stringValue(value.get("result"))
        ));
        put(normalized, "heartRateBpm", firstDouble(
                entry.get("averageHeartRate"),
                value.get("averageHeartRate"),
                entry.get("heartRate"),
                value.get("heartRate")
        ));
        put(normalized, "symptoms", stringList(firstList(entry.get("symptoms"), value.get("symptoms"))));
        put(normalized, "deviceName", firstNonBlank(stringValue(entry.get("deviceName")), stringValue(value.get("deviceName"))));
        return normalized;
    }

    private Map<String, Object> normalizeSleep(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> levels = mapValue(entry.get("levels"));
        Map<String, Object> summary = mapValue(levels.get("summary"));
        Map<String, Object> score = mapValue(entry.get("score"));
        put(normalized, "logId", stringValue(entry.get("logId")));
        put(normalized, "startTime", metricInstant(entry, timezone, "startTime"));
        put(normalized, "endTime", metricInstant(entry, timezone, "endTime"));
        put(normalized, "dateOfSleep", stringValue(entry.get("dateOfSleep")));
        put(normalized, "durationSeconds", durationSeconds(longValue(entry.get("duration"))));
        put(normalized, "timeInBedSeconds", minutesToSeconds(intValue(entry.get("timeInBed"))));
        put(normalized, "minutesAsleep", intValue(entry.get("minutesAsleep")));
        put(normalized, "minutesAwake", intValue(entry.get("minutesAwake")));
        put(normalized, "minutesAfterWakeup", intValue(entry.get("minutesAfterWakeup")));
        put(normalized, "minutesToFallAsleep", intValue(entry.get("minutesToFallAsleep")));
        put(normalized, "efficiency", intValue(entry.get("efficiency")));
        put(normalized, "mainSleep", booleanValue(entry.get("mainSleep")));
        put(normalized, "type", stringValue(entry.get("type")));
        put(normalized, "sleepScore", intValue(firstNonNull(score.get("overall"), score.get("sleepScore"))));
        put(normalized, "stages", normalizeSleepStages(summary));
        return normalized;
    }

    private Map<String, Object> normalizeBloodOxygenSaturation(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        put(normalized, "recordedAt", metricInstant(entry, timezone, "dateTime"));
        put(normalized, "averagePct", firstDouble(
                value.get("avg"),
                value.get("average"),
                value.get("spo2"),
                entry.get("value")
        ));
        put(normalized, "minPct", firstDouble(value.get("min"), value.get("minimum")));
        put(normalized, "maxPct", firstDouble(value.get("max"), value.get("maximum")));
        return normalized;
    }

    private Map<String, Object> normalizeHeartRateVariability(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        put(normalized, "recordedAt", metricInstant(entry, timezone, "dateTime"));
        put(normalized, "dailyRmssd", firstDouble(value.get("dailyRmssd"), entry.get("dailyRmssd")));
        put(normalized, "deepRmssd", firstDouble(value.get("deepRmssd"), entry.get("deepRmssd")));
        put(normalized, "coverage", firstDouble(value.get("coverage"), entry.get("coverage")));
        return normalized;
    }

    private Map<String, Object> normalizeTemperature(Map<String, Object> entry, String timezone, String preferredValueKey) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        put(normalized, "recordedAt", metricInstant(entry, timezone, "dateTime"));
        put(normalized, "temperatureC", firstDouble(
                value.get(preferredValueKey),
                value.get("value"),
                value.get("temperature"),
                entry.get(preferredValueKey),
                entry.get("value")
        ));
        put(normalized, "nightlyRelativeC", firstDouble(
                value.get("nightlyRelative"),
                value.get("relativeDeviation"),
                entry.get("nightlyRelative")
        ));
        return normalized;
    }

    private Map<String, Object> normalizeIrregularRhythmNotification(Map<String, Object> entry, String timezone) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        Map<String, Object> value = metricValue(entry);
        put(normalized, "detectedAt", metricInstant(entry, timezone, "startTime", "dateTime", "timestamp"));
        put(normalized, "alertedAt", metricInstant(entry, timezone, "alertedAt"));
        put(normalized, "status", firstNonBlank(
                stringValue(entry.get("status")),
                stringValue(value.get("status"))
        ));
        put(normalized, "result", firstNonBlank(
                stringValue(entry.get("result")),
                stringValue(value.get("result")),
                stringValue(entry.get("classification"))
        ));
        return normalized;
    }

    private Map<String, Object> normalizeSleepStages(Map<String, Object> summary) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        put(normalized, "deep", normalizeSleepStage(mapValue(summary.get("deep"))));
        put(normalized, "light", normalizeSleepStage(mapValue(summary.get("light"))));
        put(normalized, "rem", normalizeSleepStage(mapValue(summary.get("rem"))));
        put(normalized, "wake", normalizeSleepStage(mapValue(summary.get("wake"))));
        put(normalized, "asleep", normalizeSleepStage(mapValue(summary.get("asleep"))));
        put(normalized, "awake", normalizeSleepStage(mapValue(summary.get("awake"))));
        put(normalized, "restless", normalizeSleepStage(mapValue(summary.get("restless"))));
        return normalized;
    }

    private Map<String, Object> normalizeSleepStage(Map<String, Object> stage) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        put(normalized, "count", intValue(stage.get("count")));
        put(normalized, "minutes", intValue(stage.get("minutes")));
        put(normalized, "thirtyDayAvgMinutes", intValue(stage.get("thirtyDayAvgMinutes")));
        return normalized;
    }

    private Map<String, Object> metricValue(Map<String, Object> entry) {
        Map<String, Object> value = mapValue(entry.get("value"));
        if (!value.isEmpty()) {
            return value;
        }
        return mapValue(entry.get("summary"));
    }

    private Object irnProfileValue(Map<String, Object> irnProfile, String... keys) {
        for (String key : keys) {
            Object value = irnProfile.get(key);
            if (value != null) {
                return value;
            }
        }
        Map<String, Object> profile = mapValue(irnProfile.get("profile"));
        for (String key : keys) {
            Object value = profile.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<Map<String, Object>> entries(Map<String, Object> payload, String... preferredKeys) {
        for (String key : preferredKeys) {
            List<Map<String, Object>> entries = listOfMaps(payload.get(key));
            if (!entries.isEmpty()) {
                return entries;
            }
        }
        return payload.values().stream()
                .map(PayloadSupportEntry::maps)
                .filter(values -> !values.isEmpty())
                .findFirst()
                .orElse(List.of());
    }

    private Map<String, Object> latestEntry(List<Map<String, Object>> entries, String timezone, String... timeKeys) {
        List<Map<String, Object>> candidates = entries.stream()
                .map(PayloadSupportEntry::map)
                .filter(entry -> !entry.isEmpty())
                .toList();

        Map<String, Object> latest = candidates.stream()
                .filter(entry -> metricInstant(entry, timezone, timeKeys) != null)
                .max(Comparator.comparing(entry -> metricInstant(entry, timezone, timeKeys)))
                .orElse(Map.of());

        if (!latest.isEmpty()) {
            return latest;
        }
        return candidates.isEmpty() ? Map.of() : candidates.get(candidates.size() - 1);
    }

    private Instant metricInstant(Map<String, Object> entry, String timezone, String... keys) {
        for (String key : keys) {
            Instant instant = parseMetricInstant(entry.get(key), timezone);
            if (instant != null) {
                return instant;
            }
        }
        Map<String, Object> value = metricValue(entry);
        for (String key : keys) {
            Instant instant = parseMetricInstant(value.get(key), timezone);
            if (instant != null) {
                return instant;
            }
        }
        return null;
    }

    private Instant parseMetricInstant(Object value, String timezone) {
        Instant instant = instantValue(value);
        if (instant != null) {
            return instant;
        }
        String string = stringValue(value);
        if (string == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(string).atZone(resolveZoneId(timezone)).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(string).atStartOfDay(resolveZoneId(timezone)).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private ZoneId resolveZoneId(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ignored) {
            return ZoneId.of("UTC");
        }
    }

    private Integer durationSeconds(Long durationMillis) {
        return durationMillis != null ? Math.toIntExact(durationMillis / 1000L) : null;
    }

    private Integer minutesToSeconds(Integer minutes) {
        return minutes != null ? minutes * 60 : null;
    }

    private Double firstDouble(Object... values) {
        for (Object value : values) {
            Double candidate = doubleValue(value);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Long firstLong(Object... values) {
        for (Object value : values) {
            Long candidate = longValue(value);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Boolean firstBoolean(Object... values) {
        for (Object value : values) {
            Boolean candidate = booleanValue(value);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<?> firstList(Object... values) {
        for (Object value : values) {
            List<?> list = listValue(value);
            if (!list.isEmpty()) {
                return list;
            }
        }
        return List.of();
    }

    private List<String> stringList(List<?> values) {
        return values.stream()
                .map(PayloadSupportEntry::string)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String preferredAvatar(Map<String, Object> user) {
        return firstNonBlank(
                stringValue(user.get("avatar640")),
                firstNonBlank(
                        stringValue(user.get("avatar150")),
                        stringValue(user.get("avatar"))
                )
        );
    }

    private String normalizeGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            return null;
        }
        return switch (gender.toUpperCase()) {
            case "MALE" -> "male";
            case "FEMALE" -> "female";
            case "NA" -> "unspecified";
            default -> gender.toLowerCase();
        };
    }

    private Long timezoneOffsetSeconds(Object offsetFromUtcMillis) {
        Long millis = longValue(offsetFromUtcMillis);
        return millis != null ? millis / 1000L : null;
    }

    private String measurementSystem(String distanceUnit) {
        if (!StringUtils.hasText(distanceUnit)) {
            return null;
        }
        String normalized = distanceUnit.trim().toLowerCase();
        if (normalized.contains("metric") || normalized.contains("kilometer") || normalized.equals("km")) {
            return "metric";
        }
        if (normalized.contains("us") || normalized.contains("mile") || normalized.equals("mi")) {
            return "imperial";
        }
        return normalized;
    }

    private Double weightKg(Map<String, Object> user) {
        return convertWeightToKg(doubleValue(user.get("weight")), stringValue(user.get("weightUnit")));
    }

    private Double heightCm(Map<String, Object> user) {
        return convertHeightToCm(doubleValue(user.get("height")), stringValue(user.get("heightUnit")));
    }

    private Double convertDistanceToMeters(Double distance, String unit) {
        if (distance == null) {
            return null;
        }
        if (!StringUtils.hasText(unit)) {
            return distance;
        }

        String normalizedUnit = unit.trim().toLowerCase();
        if (normalizedUnit.contains("mile") || normalizedUnit.equals("mi") || normalizedUnit.contains("us")) {
            return distance * 1609.344d;
        }
        if (normalizedUnit.contains("kilometer") || normalizedUnit.equals("km") || normalizedUnit.contains("metric")) {
            return distance * 1000d;
        }
        if (normalizedUnit.contains("meter") || normalizedUnit.equals("m")) {
            return distance;
        }
        return distance;
    }

    private Double convertWeightToKg(Double weight, String unit) {
        if (weight == null) {
            return null;
        }
        if (!StringUtils.hasText(unit)) {
            return weight;
        }

        String normalizedUnit = unit.trim().toLowerCase();
        if (normalizedUnit.contains("lb") || normalizedUnit.contains("pound")) {
            return weight * 0.45359237d;
        }
        if (normalizedUnit.contains("stone")) {
            return weight * 6.35029318d;
        }
        return weight;
    }

    private Double convertHeightToCm(Double height, String unit) {
        if (height == null) {
            return null;
        }
        if (!StringUtils.hasText(unit)) {
            return height;
        }

        String normalizedUnit = unit.trim().toLowerCase();
        if (normalizedUnit.contains("inch") || normalizedUnit.equals("in")) {
            return height * 2.54d;
        }
        if (normalizedUnit.contains("foot") || normalizedUnit.equals("ft")) {
            return height * 30.48d;
        }
        if (normalizedUnit.contains("meter") && !normalizedUnit.contains("cent")) {
            return height * 100d;
        }
        return height;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static final class PayloadSupportEntry {

        private PayloadSupportEntry() {
        }

        private static Map<String, Object> map(Object value) {
            return mapValue(value);
        }

        private static List<Map<String, Object>> maps(Object value) {
            return listOfMaps(value);
        }

        private static String string(Object value) {
            return stringValue(value);
        }
    }
}
