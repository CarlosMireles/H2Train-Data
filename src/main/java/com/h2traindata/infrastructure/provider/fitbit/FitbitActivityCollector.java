package com.h2traindata.infrastructure.provider.fitbit;

import static com.h2traindata.infrastructure.provider.common.PayloadSupport.booleanValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.doubleValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.instantValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.intValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.listOfMaps;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.longValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.mapValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.offsetDateTimeValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.put;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.stringValue;

import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.infrastructure.provider.fitbit.client.FitbitApiClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "app.fitbit", name = "enabled", havingValue = "true")
public class FitbitActivityCollector implements ProviderEventCollector {

    private static final int INITIAL_FETCH_LIMIT = 50;
    private static final int INCREMENTAL_FETCH_LIMIT = 100;

    private final FitbitApiClient fitbitApiClient;

    public FitbitActivityCollector(FitbitApiClient fitbitApiClient) {
        this.fitbitApiClient = fitbitApiClient;
    }

    @Override
    public String providerId() {
        return "fitbit";
    }

    @Override
    public EventType eventType() {
        return EventType.ACTIVITY;
    }

    @Override
    public EventBatch collect(ProviderConnection connection, SyncCursor cursor) {
        String afterDate = cursor != null && StringUtils.hasText(cursor.value()) ? cursor.value() : null;
        List<Map<String, Object>> activities = fitbitApiClient.fetchActivityLogs(
                connection.accessToken(),
                afterDate,
                afterDate == null ? LocalDate.now().toString() : null,
                afterDate == null ? INITIAL_FETCH_LIMIT : INCREMENTAL_FETCH_LIMIT
        );

        List<ProviderEvent> events = activities.stream()
                .map(activity -> toProviderEvent(connection, activity))
                .toList();

        return new EventBatch(
                providerId(),
                connection.athlete().id(),
                eventType(),
                events,
                nextCursor(activities, cursor)
        );
    }

    private ProviderEvent toProviderEvent(ProviderConnection connection, Map<String, Object> activityLog) {
        Long logId = longValue(activityLog.get("logId"));
        if (logId == null) {
            throw new IllegalStateException("Fitbit activity payload did not include a logId");
        }

        String tcxXml = fetchActivityTcx(connection.accessToken(), logId);
        FitbitTcxParser.FitbitTcxData tcxData = FitbitTcxParser.parse(tcxXml);
        Map<String, Object> workoutSummaryResponse = fetchWorkoutSummary(connection.accessToken(), logId);
        Map<String, Object> workoutSummary = mapValue(workoutSummaryResponse.get("workoutSummary"));

        return new ProviderEvent(
                providerId(),
                connection.athlete().id(),
                eventType(),
                String.valueOf(logId),
                occurredAt(activityLog),
                normalizedPayload(activityLog, tcxData, workoutSummary),
                providerSpecificPayload(activityLog, tcxData, workoutSummary),
                rawPayload(activityLog, workoutSummaryResponse, tcxXml)
        );
    }

    private Map<String, Object> normalizedPayload(Map<String, Object> activityLog,
                                                  FitbitTcxParser.FitbitTcxData tcxData,
                                                  Map<String, Object> workoutSummary) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        OffsetDateTime startTime = offsetDateTimeValue(activityLog.get("startTime"));
        String activityName = stringValue(activityLog.get("activityName"));
        String sport = normalizedSport(activityName);
        String subSport = activityName != null && !activityName.equalsIgnoreCase(sport) ? activityName : null;
        Map<String, Object> source = mapValue(activityLog.get("source"));
        Map<String, Object> mobilityMetrics = mapValue(workoutSummary.get("mobilityMetrics"));
        Double distanceMeters = resolvedDistanceMeters(activityLog, tcxData);
        Integer movingTimeSeconds = durationSeconds(activityLog.get("activeDuration"));
        Integer elapsedTimeSeconds = durationSeconds(activityLog.get("duration"));

        put(normalized, "providerActivityId", stringValue(activityLog.get("logId")));
        put(normalized, "name", activityName);
        put(normalized, "sport", sport);
        put(normalized, "subSport", subSport);
        put(normalized, "manual", isManual(activityLog));
        put(normalized, "startTime", startTime != null ? startTime.toInstant() : null);
        put(normalized, "startTimeLocal", stringValue(activityLog.get("startTime")));
        put(normalized, "timezone", startTime != null ? startTime.getOffset().getId() : null);
        put(normalized, "distanceMeters", distanceMeters);
        put(normalized, "movingTimeSeconds", movingTimeSeconds);
        put(normalized, "elapsedTimeSeconds", elapsedTimeSeconds);
        put(normalized, "steps", intValue(activityLog.get("steps")));
        put(normalized, "caloriesKcal", doubleValue(activityLog.get("calories")));
        put(normalized, "elevationGainMeters", doubleValue(activityLog.get("elevationGain")));
        put(normalized, "averageSpeedMps", averageSpeed(distanceMeters, movingTimeSeconds, elapsedTimeSeconds));
        put(normalized, "averageHeartRateBpm", doubleValue(activityLog.get("averageHeartRate")));
        put(normalized, "maxHeartRateBpm", tcxData.maxHeartRateBpm() != null ? tcxData.maxHeartRateBpm().doubleValue() : null);
        put(normalized, "averageCadenceRpm", doubleValue(mobilityMetrics.get("avgCadenceStepsPerMinute")));
        put(normalized, "deviceName", stringValue(source.get("name")));
        put(normalized, "startLatitude", tcxData.startLatitude());
        put(normalized, "startLongitude", tcxData.startLongitude());
        put(normalized, "endLatitude", tcxData.endLatitude());
        put(normalized, "endLongitude", tcxData.endLongitude());
        put(normalized, "elevationHighMeters", tcxData.maxAltitudeMeters());
        put(normalized, "elevationLowMeters", tcxData.minAltitudeMeters());
        put(normalized, "laps", tcxData.laps());
        put(normalized, "trackpoints", tcxData.trackpoints());

        return normalized;
    }

    private Map<String, Object> providerSpecificPayload(Map<String, Object> activityLog,
                                                        FitbitTcxParser.FitbitTcxData tcxData,
                                                        Map<String, Object> workoutSummary) {
        Map<String, Object> providerSpecific = new LinkedHashMap<>();
        put(providerSpecific, "activityTypeId", intValue(activityLog.get("activityTypeId")));
        put(providerSpecific, "logType", stringValue(activityLog.get("logType")));
        put(providerSpecific, "activityLevel", listOfMaps(activityLog.get("activityLevel")));
        put(providerSpecific, "activeZoneMinutes", mapValue(activityLog.get("activeZoneMinutes")));
        put(providerSpecific, "hasActiveZoneMinutes", booleanValue(activityLog.get("hasActiveZoneMinutes")));
        put(providerSpecific, "heartRateZones", listOfMaps(activityLog.get("heartRateZones")));
        put(providerSpecific, "heartRateLink", stringValue(activityLog.get("heartRateLink")));
        put(providerSpecific, "caloriesLink", stringValue(activityLog.get("caloriesLink")));
        put(providerSpecific, "detailsLink", stringValue(activityLog.get("detailsLink")));
        put(providerSpecific, "manualValuesSpecified", mapValue(activityLog.get("manualValuesSpecified")));
        put(providerSpecific, "source", mapValue(activityLog.get("source")));
        put(providerSpecific, "distanceUnit", stringValue(activityLog.get("distanceUnit")));
        put(providerSpecific, "pace", doubleValue(activityLog.get("pace")));
        put(providerSpecific, "speed", doubleValue(activityLog.get("speed")));
        put(providerSpecific, "lastModified", instantValue(activityLog.get("lastModified")));
        put(providerSpecific, "originalDurationMs", longValue(activityLog.get("originalDuration")));
        put(providerSpecific, "originalStartTime", stringValue(activityLog.get("originalStartTime")));
        put(providerSpecific, "activeDurationMs", longValue(activityLog.get("activeDuration")));
        put(providerSpecific, "workoutSummary", workoutSummary);
        put(providerSpecific, "tcxSport", tcxData.sport());
        put(providerSpecific, "tcxTrackpointCount", tcxData.trackpoints().size());
        put(providerSpecific, "tcxLapCount", tcxData.laps().size());
        return providerSpecific;
    }

    private Map<String, Object> rawPayload(Map<String, Object> activityLog,
                                           Map<String, Object> workoutSummaryResponse,
                                           String tcxXml) {
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        put(rawPayload, "activityLog", activityLog);
        put(rawPayload, "workoutSummary", workoutSummaryResponse);
        put(rawPayload, "tcxXml", tcxXml);
        return rawPayload;
    }

    private Instant occurredAt(Map<String, Object> activityLog) {
        OffsetDateTime startTime = offsetDateTimeValue(activityLog.get("startTime"));
        if (startTime != null) {
            return startTime.toInstant();
        }
        OffsetDateTime originalStartTime = offsetDateTimeValue(activityLog.get("originalStartTime"));
        return originalStartTime != null ? originalStartTime.toInstant() : Instant.now();
    }

    private String fetchActivityTcx(String accessToken, long logId) {
        try {
            return fitbitApiClient.fetchActivityTcx(accessToken, logId);
        } catch (RestClientException ignored) {
            return null;
        }
    }

    private Map<String, Object> fetchWorkoutSummary(String accessToken, long logId) {
        try {
            Map<String, Object> response = fitbitApiClient.fetchWorkoutSummary(accessToken, logId);
            return response != null ? response : Map.of();
        } catch (RestClientException ignored) {
            return Map.of();
        }
    }

    private Double resolvedDistanceMeters(Map<String, Object> activityLog, FitbitTcxParser.FitbitTcxData tcxData) {
        if (tcxData.totalDistanceMeters() != null) {
            return tcxData.totalDistanceMeters();
        }
        return convertDistanceToMeters(doubleValue(activityLog.get("distance")), stringValue(activityLog.get("distanceUnit")));
    }

    private Double averageSpeed(Double distanceMeters, Integer movingTimeSeconds, Integer elapsedTimeSeconds) {
        Integer denominator = movingTimeSeconds != null && movingTimeSeconds > 0 ? movingTimeSeconds : elapsedTimeSeconds;
        if (distanceMeters == null || denominator == null || denominator == 0) {
            return null;
        }
        return distanceMeters / denominator;
    }

    private Integer durationSeconds(Object durationMilliseconds) {
        Long duration = longValue(durationMilliseconds);
        return duration != null ? Math.toIntExact(duration / 1000L) : null;
    }

    private boolean isManual(Map<String, Object> activityLog) {
        if ("manual".equalsIgnoreCase(stringValue(activityLog.get("logType")))) {
            return true;
        }
        Map<String, Object> manualValuesSpecified = mapValue(activityLog.get("manualValuesSpecified"));
        return manualValuesSpecified.values().stream()
                .map(value -> booleanValue(value))
                .anyMatch(Boolean.TRUE::equals);
    }

    private Double convertDistanceToMeters(Double distance, String distanceUnit) {
        if (distance == null) {
            return null;
        }
        if (!StringUtils.hasText(distanceUnit)) {
            return distance;
        }

        String normalizedUnit = distanceUnit.toLowerCase();
        if (normalizedUnit.contains("mile") || normalizedUnit.equals("mi")) {
            return distance * 1609.344d;
        }
        if (normalizedUnit.contains("kilometer") || normalizedUnit.equals("km")) {
            return distance * 1000d;
        }
        if (normalizedUnit.contains("meter") || normalizedUnit.equals("m")) {
            return distance;
        }
        if (normalizedUnit.contains("foot") || normalizedUnit.equals("ft")) {
            return distance * 0.3048d;
        }
        return distance;
    }

    private String normalizedSport(String activityName) {
        String rawName = activityName != null ? activityName : "Workout";
        String value = rawName.toLowerCase();
        if (value.contains("run")) {
            return "Run";
        }
        if (value.contains("walk")) {
            return "Walk";
        }
        if (value.contains("ride") || value.contains("bike") || value.contains("cycling")) {
            return "Ride";
        }
        if (value.contains("hike")) {
            return "Hike";
        }
        if (value.contains("swim")) {
            return "Swim";
        }
        if (value.contains("elliptical")) {
            return "Elliptical";
        }
        if (value.contains("yoga")) {
            return "Yoga";
        }
        if (value.contains("weight")) {
            return "WeightTraining";
        }
        return rawName;
    }

    private SyncCursor nextCursor(List<Map<String, Object>> activities, SyncCursor cursor) {
        return activities.stream()
                .map(activity -> offsetDateTimeValue(activity.get("startTime")))
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .map(value -> new SyncCursor(value.toString()))
                .orElse(cursor);
    }
}
