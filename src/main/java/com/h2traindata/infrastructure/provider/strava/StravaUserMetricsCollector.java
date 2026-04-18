package com.h2traindata.infrastructure.provider.strava;

import static com.h2traindata.infrastructure.provider.common.PayloadSupport.booleanValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.doubleValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.instantValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.intValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.listOfMaps;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.longValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.mapValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.put;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.stringValue;
import static com.h2traindata.infrastructure.provider.common.ProviderRequestSupport.getOrDefault;

import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.infrastructure.provider.strava.client.StravaApiClient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StravaUserMetricsCollector implements ProviderEventCollector {

    private final StravaApiClient stravaApiClient;

    public StravaUserMetricsCollector(StravaApiClient stravaApiClient) {
        this.stravaApiClient = stravaApiClient;
    }

    @Override
    public String providerId() {
        return "strava";
    }

    @Override
    public EventType eventType() {
        return EventType.USER_METRICS;
    }

    @Override
    public EventBatch collect(ProviderConnection connection, SyncCursor cursor) {
        Instant snapshotAt = Instant.now();
        Map<String, Object> athletePayload = fetchAthletePayload(connection.accessToken());
        String athleteId = firstNonBlank(stringValue(athletePayload.get("id")), connection.athlete().id());
        Map<String, Object> athleteStats = fetchAthleteStats(connection.accessToken(), athleteId);

        ProviderEvent event = new ProviderEvent(
                providerId(),
                connection.athlete().id(),
                eventType(),
                snapshotEventId(athleteId, snapshotAt),
                snapshotAt,
                normalizedPayload(athletePayload, athleteStats),
                providerSpecificPayload(athletePayload, athleteStats),
                rawPayload(athletePayload, athleteStats)
        );

        return new EventBatch(
                providerId(),
                connection.athlete().id(),
                eventType(),
                java.util.List.of(event),
                null
        );
    }

    private Map<String, Object> normalizedPayload(Map<String, Object> athletePayload, Map<String, Object> athleteStats) {
        Map<String, Object> normalized = new LinkedHashMap<>();

        put(normalized, "providerAthleteId", stringValue(athletePayload.get("id")));
        put(normalized, "username", stringValue(athletePayload.get("username")));
        put(normalized, "displayName", firstNonBlank(fullName(athletePayload), stringValue(athletePayload.get("username"))));
        put(normalized, "fullName", fullName(athletePayload));
        put(normalized, "firstName", stringValue(athletePayload.get("firstname")));
        put(normalized, "lastName", stringValue(athletePayload.get("lastname")));
        put(normalized, "avatarUrl", stringValue(athletePayload.get("profile")));
        put(normalized, "city", stringValue(athletePayload.get("city")));
        put(normalized, "state", stringValue(athletePayload.get("state")));
        put(normalized, "country", stringValue(athletePayload.get("country")));
        put(normalized, "gender", normalizeGender(stringValue(athletePayload.get("sex"))));
        put(normalized, "measurementSystem", measurementSystem(stringValue(athletePayload.get("measurement_preference"))));
        put(normalized, "createdAt", instantValue(athletePayload.get("created_at")));
        put(normalized, "updatedAt", instantValue(athletePayload.get("updated_at")));
        put(normalized, "weightKg", doubleValue(athletePayload.get("weight")));
        put(normalized, "followerCount", intValue(athletePayload.get("follower_count")));
        put(normalized, "friendCount", intValue(athletePayload.get("friend_count")));
        put(normalized, "subscriptionActive", summitActive(athletePayload));
        put(normalized, "ftpWatts", intValue(athletePayload.get("ftp")));
        put(normalized, "lifetimeDistanceMeters", totalLifetimeDistanceMeters(athleteStats));
        return normalized;
    }

    private Map<String, Object> providerSpecificPayload(Map<String, Object> athletePayload, Map<String, Object> athleteStats) {
        Map<String, Object> providerSpecific = new LinkedHashMap<>();
        put(providerSpecific, "resourceState", intValue(athletePayload.get("resource_state")));
        put(providerSpecific, "profileMedium", stringValue(athletePayload.get("profile_medium")));
        put(providerSpecific, "measurementPreference", stringValue(athletePayload.get("measurement_preference")));
        put(providerSpecific, "premium", booleanValue(athletePayload.get("premium")));
        put(providerSpecific, "summit", booleanValue(athletePayload.get("summit")));
        put(providerSpecific, "clubs", listOfMaps(athletePayload.get("clubs")));
        put(providerSpecific, "bikes", listOfMaps(athletePayload.get("bikes")));
        put(providerSpecific, "shoes", listOfMaps(athletePayload.get("shoes")));
        put(providerSpecific, "activityStats", normalizeAthleteStats(athleteStats));
        return providerSpecific;
    }

    private Map<String, Object> rawPayload(Map<String, Object> athletePayload, Map<String, Object> athleteStats) {
        Map<String, Object> rawPayload = new LinkedHashMap<>();
        put(rawPayload, "athlete", athletePayload);
        put(rawPayload, "stats", athleteStats);
        return rawPayload;
    }

    private Map<String, Object> fetchAthletePayload(String accessToken) {
        return getOrDefault(() -> stravaApiClient.fetchAthletePayload(accessToken), Map.of());
    }

    private Map<String, Object> fetchAthleteStats(String accessToken, String athleteId) {
        Long athleteIdValue = longValue(athleteId);
        if (athleteIdValue == null) {
            return Map.of();
        }
        return getOrDefault(() -> stravaApiClient.fetchAthleteStats(accessToken, athleteIdValue), Map.of());
    }

    private String snapshotEventId(String athleteId, Instant snapshotAt) {
        return athleteId + ":" + snapshotAt.toEpochMilli();
    }

    private String fullName(Map<String, Object> athletePayload) {
        String firstName = stringValue(athletePayload.get("firstname"));
        String lastName = stringValue(athletePayload.get("lastname"));
        if (!StringUtils.hasText(firstName) && !StringUtils.hasText(lastName)) {
            return null;
        }
        if (!StringUtils.hasText(firstName)) {
            return lastName;
        }
        if (!StringUtils.hasText(lastName)) {
            return firstName;
        }
        return firstName + " " + lastName;
    }

    private String normalizeGender(String sex) {
        if (!StringUtils.hasText(sex)) {
            return null;
        }
        return switch (sex.toUpperCase()) {
            case "M" -> "male";
            case "F" -> "female";
            default -> sex.toLowerCase();
        };
    }

    private String measurementSystem(String measurementPreference) {
        if (!StringUtils.hasText(measurementPreference)) {
            return null;
        }
        return switch (measurementPreference.toLowerCase()) {
            case "meters" -> "metric";
            case "feet" -> "imperial";
            default -> measurementPreference.toLowerCase();
        };
    }

    private Boolean summitActive(Map<String, Object> athletePayload) {
        Boolean summit = booleanValue(athletePayload.get("summit"));
        return summit != null ? summit : booleanValue(athletePayload.get("premium"));
    }

    private Double totalLifetimeDistanceMeters(Map<String, Object> athleteStats) {
        return sum(
                distance(mapValue(athleteStats.get("all_run_totals"))),
                distance(mapValue(athleteStats.get("all_ride_totals"))),
                distance(mapValue(athleteStats.get("all_swim_totals")))
        );
    }

    private Map<String, Object> normalizeAthleteStats(Map<String, Object> athleteStats) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        put(normalized, "recent", totalsByWindow(athleteStats, "recent"));
        put(normalized, "yearToDate", totalsByWindow(athleteStats, "ytd"));
        put(normalized, "allTime", totalsByWindow(athleteStats, "all"));
        put(normalized, "biggestRideDistanceMeters", doubleValue(athleteStats.get("biggest_ride_distance")));
        put(normalized, "biggestClimbElevationGainMeters", doubleValue(athleteStats.get("biggest_climb_elevation_gain")));
        return normalized;
    }

    private Map<String, Object> totalsByWindow(Map<String, Object> athleteStats, String windowPrefix) {
        Map<String, Object> totals = new LinkedHashMap<>();
        put(totals, "run", normalizeActivityTotal(mapValue(athleteStats.get(windowPrefix + "_run_totals"))));
        put(totals, "ride", normalizeActivityTotal(mapValue(athleteStats.get(windowPrefix + "_ride_totals"))));
        put(totals, "swim", normalizeActivityTotal(mapValue(athleteStats.get(windowPrefix + "_swim_totals"))));
        return totals;
    }

    private Map<String, Object> normalizeActivityTotal(Map<String, Object> total) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        put(normalized, "count", intValue(total.get("count")));
        put(normalized, "distanceMeters", distance(total));
        put(normalized, "movingTimeSeconds", intValue(total.get("moving_time")));
        put(normalized, "elapsedTimeSeconds", intValue(total.get("elapsed_time")));
        put(normalized, "elevationGainMeters", doubleValue(total.get("elevation_gain")));
        put(normalized, "achievementCount", intValue(total.get("achievement_count")));
        return normalized;
    }

    private Double distance(Map<String, Object> total) {
        return doubleValue(total.get("distance"));
    }

    private Double sum(Double... values) {
        double total = 0d;
        boolean hasValue = false;
        for (Double value : values) {
            if (value == null) {
                continue;
            }
            total += value;
            hasValue = true;
        }
        return hasValue ? total : null;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
