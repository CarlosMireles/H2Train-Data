package com.h2traindata.infrastructure.provider.strava;

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

import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.infrastructure.provider.strava.client.StravaApiClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Component
public class StravaActivityCollector implements ProviderEventCollector {

    private static final int PAGE_SIZE = 50;

    private final StravaApiClient stravaApiClient;

    public StravaActivityCollector(StravaApiClient stravaApiClient) {
        this.stravaApiClient = stravaApiClient;
    }

    @Override
    public String providerId() {
        return "strava";
    }

    @Override
    public EventType eventType() {
        return EventType.ACTIVITY;
    }

    @Override
    public EventBatch collect(ProviderConnection connection, SyncCursor cursor) {
        List<Map<String, Object>> activities = stravaApiClient.fetchActivities(
                connection.accessToken(),
                PAGE_SIZE,
                resolveAfterEpoch(cursor)
        );

        List<ProviderEvent> events = activities.stream()
                .map(activity -> toProviderEvent(connection, activity))
                .toList();

        return new EventBatch(
                providerId(),
                connection.athlete().id(),
                eventType(),
                events,
                nextCursor(events, cursor)
        );
    }

    private ProviderEvent toProviderEvent(ProviderConnection connection, Map<String, Object> summaryActivity) {
        Long activityId = longValue(summaryActivity.get("id"));
        if (activityId == null) {
            throw new IllegalStateException("Strava activity payload did not include an id");
        }

        Map<String, Object> detailActivity = fetchDetailedActivity(connection.accessToken(), activityId, summaryActivity);
        Map<String, Object> streams = fetchStreams(connection.accessToken(), activityId);
        List<Map<String, Object>> zones = fetchZones(connection.accessToken(), activityId);

        Map<String, Object> normalizedPayload = normalizedPayload(detailActivity, streams);
        Map<String, Object> providerSpecificPayload = providerSpecificPayload(detailActivity, streams, zones);
        Map<String, Object> rawPayload = rawPayload(summaryActivity, detailActivity, streams, zones);
        Instant occurredAt = instantValue(detailActivity.get("start_date"));

        return new ProviderEvent(
                providerId(),
                connection.athlete().id(),
                eventType(),
                String.valueOf(activityId),
                occurredAt,
                normalizedPayload,
                providerSpecificPayload,
                rawPayload
        );
    }

    private Map<String, Object> normalizedPayload(Map<String, Object> detailActivity, Map<String, Object> streams) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        List<Map<String, Object>> trackpoints = buildTrackpoints(detailActivity, streams);

        String sportType = stringValue(detailActivity.get("sport_type"));
        String legacyType = stringValue(detailActivity.get("type"));

        put(normalized, "providerActivityId", stringValue(detailActivity.get("id")));
        put(normalized, "externalId", stringValue(detailActivity.get("external_id")));
        put(normalized, "name", stringValue(detailActivity.get("name")));
        put(normalized, "description", stringValue(detailActivity.get("description")));
        put(normalized, "sport", StringUtils.hasText(sportType) ? sportType : legacyType);
        put(normalized, "subSport", StringUtils.hasText(sportType) && !sportType.equals(legacyType) ? legacyType : null);
        put(normalized, "manual", booleanValue(detailActivity.get("manual")));
        put(normalized, "private", booleanValue(detailActivity.get("private")));
        put(normalized, "trainer", booleanValue(detailActivity.get("trainer")));
        put(normalized, "commute", booleanValue(detailActivity.get("commute")));
        put(normalized, "startTime", instantValue(detailActivity.get("start_date")));
        put(normalized, "startTimeLocal", stringValue(detailActivity.get("start_date_local")));
        put(normalized, "timezone", stringValue(detailActivity.get("timezone")));
        put(normalized, "distanceMeters", doubleValue(detailActivity.get("distance")));
        put(normalized, "movingTimeSeconds", intValue(detailActivity.get("moving_time")));
        put(normalized, "elapsedTimeSeconds", intValue(detailActivity.get("elapsed_time")));
        put(normalized, "caloriesKcal", doubleValue(detailActivity.get("calories")));
        put(normalized, "elevationGainMeters", doubleValue(detailActivity.get("total_elevation_gain")));
        put(normalized, "elevationHighMeters", doubleValue(detailActivity.get("elev_high")));
        put(normalized, "elevationLowMeters", doubleValue(detailActivity.get("elev_low")));
        put(normalized, "averageSpeedMps", doubleValue(detailActivity.get("average_speed")));
        put(normalized, "maxSpeedMps", doubleValue(detailActivity.get("max_speed")));
        put(normalized, "averageHeartRateBpm", doubleValue(detailActivity.get("average_heartrate")));
        put(normalized, "maxHeartRateBpm", maxHeartRate(detailActivity, trackpoints));
        put(normalized, "averageCadenceRpm", doubleValue(detailActivity.get("average_cadence")));
        put(normalized, "averagePowerWatts", doubleValue(detailActivity.get("average_watts")));
        put(normalized, "maxPowerWatts", intValue(detailActivity.get("max_watts")));
        put(normalized, "deviceName", stringValue(detailActivity.get("device_name")));
        put(normalized, "gearId", stringValue(detailActivity.get("gear_id")));
        put(normalized, "startLatitude", coordinate(detailActivity.get("start_latlng"), 0));
        put(normalized, "startLongitude", coordinate(detailActivity.get("start_latlng"), 1));
        put(normalized, "endLatitude", coordinate(detailActivity.get("end_latlng"), 0));
        put(normalized, "endLongitude", coordinate(detailActivity.get("end_latlng"), 1));
        put(normalized, "mapPolyline", stringValue(mapValue(detailActivity.get("map")).get("polyline")));
        put(normalized, "summaryPolyline", stringValue(mapValue(detailActivity.get("map")).get("summary_polyline")));
        put(normalized, "laps", buildLaps(detailActivity.get("laps")));
        put(normalized, "splits", buildSplits(detailActivity.get("splits_metric")));
        put(normalized, "trackpoints", trackpoints);

        return normalized;
    }

    private Map<String, Object> providerSpecificPayload(Map<String, Object> detailActivity,
                                                        Map<String, Object> streams,
                                                        List<Map<String, Object>> zones) {
        Map<String, Object> providerSpecific = new LinkedHashMap<>();
        put(providerSpecific, "legacyType", stringValue(detailActivity.get("type")));
        put(providerSpecific, "resourceState", intValue(detailActivity.get("resource_state")));
        put(providerSpecific, "uploadId", longValue(detailActivity.get("upload_id")));
        put(providerSpecific, "uploadIdStr", stringValue(detailActivity.get("upload_id_str")));
        put(providerSpecific, "achievementCount", intValue(detailActivity.get("achievement_count")));
        put(providerSpecific, "kudosCount", intValue(detailActivity.get("kudos_count")));
        put(providerSpecific, "commentCount", intValue(detailActivity.get("comment_count")));
        put(providerSpecific, "athleteCount", intValue(detailActivity.get("athlete_count")));
        put(providerSpecific, "photoCount", intValue(detailActivity.get("photo_count")));
        put(providerSpecific, "totalPhotoCount", intValue(detailActivity.get("total_photo_count")));
        put(providerSpecific, "hasKudoed", booleanValue(detailActivity.get("has_kudoed")));
        put(providerSpecific, "flagged", booleanValue(detailActivity.get("flagged")));
        put(providerSpecific, "workoutType", intValue(detailActivity.get("workout_type")));
        put(providerSpecific, "kilojoules", doubleValue(detailActivity.get("kilojoules")));
        put(providerSpecific, "deviceWatts", booleanValue(detailActivity.get("device_watts")));
        put(providerSpecific, "weightedAverageWatts", intValue(detailActivity.get("weighted_average_watts")));
        put(providerSpecific, "hideFromHome", booleanValue(detailActivity.get("hide_from_home")));
        put(providerSpecific, "segmentLeaderBoardOptOut", booleanValue(detailActivity.get("segment_leaderboard_opt_out")));
        put(providerSpecific, "leaderboardOptOut", booleanValue(detailActivity.get("leaderboard_opt_out")));
        put(providerSpecific, "gear", mapValue(detailActivity.get("gear")));
        put(providerSpecific, "photos", mapValue(detailActivity.get("photos")));
        put(providerSpecific, "segmentEfforts", listOfMaps(detailActivity.get("segment_efforts")));
        put(providerSpecific, "bestEfforts", listOfMaps(detailActivity.get("best_efforts")));
        put(providerSpecific, "splitsStandard", buildSplits(detailActivity.get("splits_standard")));
        put(providerSpecific, "zones", zones);
        put(providerSpecific, "availableStreamTypes", new ArrayList<>(streams.keySet()));
        return providerSpecific;
    }

    private Map<String, Object> rawPayload(Map<String, Object> summaryActivity,
                                           Map<String, Object> detailActivity,
                                           Map<String, Object> streams,
                                           List<Map<String, Object>> zones) {
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        put(rawPayload, "summaryActivity", summaryActivity);
        put(rawPayload, "detailedActivity", detailActivity);
        put(rawPayload, "streams", streams);
        put(rawPayload, "zones", zones);
        return rawPayload;
    }

    private List<Map<String, Object>> buildTrackpoints(Map<String, Object> detailActivity, Map<String, Object> streams) {
        List<?> times = streamData(streams, "time");
        List<?> distances = streamData(streams, "distance");
        List<?> latlng = streamData(streams, "latlng");
        List<?> altitude = streamData(streams, "altitude");
        List<?> velocity = streamData(streams, "velocity_smooth");
        List<?> heartrate = streamData(streams, "heartrate");
        List<?> cadence = streamData(streams, "cadence");
        List<?> watts = streamData(streams, "watts");
        List<?> temperature = streamData(streams, "temp");
        List<?> moving = streamData(streams, "moving");
        List<?> grade = streamData(streams, "grade_smooth");
        Instant startTime = instantValue(detailActivity.get("start_date"));

        int size = List.of(times, distances, latlng, altitude, velocity, heartrate, cadence, watts, temperature, moving, grade)
                .stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        List<Map<String, Object>> trackpoints = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            Map<String, Object> point = new LinkedHashMap<>();
            Integer offsetSeconds = valueAt(times, index, Integer.class);
            put(point, "offsetSeconds", offsetSeconds);
            put(point, "recordedAt", startTime != null && offsetSeconds != null ? startTime.plusSeconds(offsetSeconds) : null);
            put(point, "distanceMeters", valueAt(distances, index, Double.class));
            put(point, "latitude", coordinateAt(latlng, index, 0));
            put(point, "longitude", coordinateAt(latlng, index, 1));
            put(point, "altitudeMeters", valueAt(altitude, index, Double.class));
            put(point, "speedMps", valueAt(velocity, index, Double.class));
            put(point, "heartRateBpm", valueAt(heartrate, index, Integer.class));
            put(point, "cadenceRpm", valueAt(cadence, index, Double.class));
            put(point, "powerWatts", valueAt(watts, index, Integer.class));
            put(point, "temperatureC", valueAt(temperature, index, Integer.class));
            put(point, "moving", valueAt(moving, index, Boolean.class));
            put(point, "gradePercent", valueAt(grade, index, Double.class));

            if (!point.isEmpty()) {
                trackpoints.add(point);
            }
        }
        return trackpoints;
    }

    private List<Map<String, Object>> buildLaps(Object lapsValue) {
        return listOfMaps(lapsValue).stream()
                .map(lap -> {
                    Map<String, Object> normalizedLap = new LinkedHashMap<>();
                    put(normalizedLap, "lapId", stringValue(lap.get("id")));
                    put(normalizedLap, "name", stringValue(lap.get("name")));
                    put(normalizedLap, "startTime", instantValue(lap.get("start_date")));
                    put(normalizedLap, "elapsedTimeSeconds", intValue(lap.get("elapsed_time")));
                    put(normalizedLap, "movingTimeSeconds", intValue(lap.get("moving_time")));
                    put(normalizedLap, "distanceMeters", doubleValue(lap.get("distance")));
                    put(normalizedLap, "averageSpeedMps", doubleValue(lap.get("average_speed")));
                    put(normalizedLap, "averageHeartRateBpm", doubleValue(lap.get("average_heartrate")));
                    put(normalizedLap, "maxHeartRateBpm", intValue(lap.get("max_heartrate")));
                    put(normalizedLap, "averageCadenceRpm", doubleValue(lap.get("average_cadence")));
                    put(normalizedLap, "averagePowerWatts", doubleValue(lap.get("average_watts")));
                    put(normalizedLap, "elevationGainMeters", doubleValue(lap.get("total_elevation_gain")));
                    return normalizedLap;
                })
                .filter(lap -> !lap.isEmpty())
                .toList();
    }

    private List<Map<String, Object>> buildSplits(Object splitsValue) {
        return listOfMaps(splitsValue).stream()
                .map(split -> {
                    Map<String, Object> normalizedSplit = new LinkedHashMap<>();
                    put(normalizedSplit, "splitIndex", intValue(split.get("split")));
                    put(normalizedSplit, "distanceMeters", doubleValue(split.get("distance")));
                    put(normalizedSplit, "elapsedTimeSeconds", intValue(split.get("elapsed_time")));
                    put(normalizedSplit, "movingTimeSeconds", intValue(split.get("moving_time")));
                    put(normalizedSplit, "averageSpeedMps", doubleValue(split.get("average_speed")));
                    put(normalizedSplit, "elevationDifferenceMeters", doubleValue(split.get("elevation_difference")));
                    put(normalizedSplit, "paceZone", intValue(split.get("pace_zone")));
                    return normalizedSplit;
                })
                .filter(split -> !split.isEmpty())
                .toList();
    }

    private List<?> streamData(Map<String, Object> streams, String streamType) {
        return listValue(mapValue(streams.get(streamType)).get("data"));
    }

    private Double maxHeartRate(Map<String, Object> detailActivity, List<Map<String, Object>> trackpoints) {
        Double explicit = doubleValue(detailActivity.get("max_heartrate"));
        if (explicit != null) {
            return explicit;
        }
        return trackpoints.stream()
                .map(point -> intValue(point.get("heartRateBpm")))
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .map(Integer::doubleValue)
                .orElse(null);
    }

    private Map<String, Object> fetchDetailedActivity(String accessToken, long activityId, Map<String, Object> fallbackSummary) {
        try {
            return stravaApiClient.fetchActivity(accessToken, activityId);
        } catch (RestClientException ignored) {
            return fallbackSummary;
        }
    }

    private Map<String, Object> fetchStreams(String accessToken, long activityId) {
        try {
            Map<String, Object> streams = stravaApiClient.fetchActivityStreams(accessToken, activityId);
            return streams != null ? streams : Map.of();
        } catch (RestClientException ignored) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> fetchZones(String accessToken, long activityId) {
        try {
            return stravaApiClient.fetchActivityZones(accessToken, activityId);
        } catch (RestClientException ignored) {
            return List.of();
        }
    }

    private Double coordinate(Object latlngValue, int coordinateIndex) {
        if (latlngValue == null) {
            return null;
        }
        return coordinateAt(List.of(latlngValue), 0, coordinateIndex);
    }

    private Double coordinateAt(List<?> coordinates, int itemIndex, int coordinateIndex) {
        if (itemIndex >= coordinates.size()) {
            return null;
        }
        Object rawValue = coordinates.get(itemIndex);
        List<?> pair = listValue(rawValue);
        if (coordinateIndex >= pair.size()) {
            return null;
        }
        return doubleValue(pair.get(coordinateIndex));
    }

    @SuppressWarnings("unchecked")
    private <T> T valueAt(List<?> values, int index, Class<T> targetType) {
        if (index >= values.size()) {
            return null;
        }
        Object value = values.get(index);
        Object converted;
        if (targetType == Integer.class) {
            converted = intValue(value);
        } else if (targetType == Double.class) {
            converted = doubleValue(value);
        } else if (targetType == Boolean.class) {
            converted = booleanValue(value);
        } else {
            converted = value;
        }
        return (T) converted;
    }

    private Long resolveAfterEpoch(SyncCursor cursor) {
        if (cursor == null || !StringUtils.hasText(cursor.value())) {
            return null;
        }
        try {
            return Long.parseLong(cursor.value());
        } catch (NumberFormatException ignored) {
            Instant instant = instantValue(cursor.value());
            return instant != null ? instant.getEpochSecond() : null;
        }
    }

    private SyncCursor nextCursor(List<ProviderEvent> events, SyncCursor cursor) {
        return events.stream()
                .map(ProviderEvent::occurredAt)
                .filter(instant -> instant != null)
                .max(Comparator.naturalOrder())
                .map(instant -> new SyncCursor(String.valueOf(instant.getEpochSecond())))
                .orElse(cursor);
    }
}
