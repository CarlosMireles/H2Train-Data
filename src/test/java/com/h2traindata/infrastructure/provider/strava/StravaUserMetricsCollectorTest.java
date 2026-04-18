package com.h2traindata.infrastructure.provider.strava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.infrastructure.provider.strava.client.StravaApiClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StravaUserMetricsCollectorTest {

    private final StravaApiClient stravaApiClient = Mockito.mock(StravaApiClient.class);
    private final StravaUserMetricsCollector collector = new StravaUserMetricsCollector(stravaApiClient);

    @Test
    void mapsAthleteProfileAndStatsToUserMetricsSnapshot() {
        ProviderConnection connection = new ProviderConnection(
                "strava",
                new AthleteProfile("99", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600)
        );

        when(stravaApiClient.fetchAthletePayload("access-token")).thenReturn(Map.ofEntries(
                Map.entry("id", 99L),
                Map.entry("username", "carlos-runner"),
                Map.entry("firstname", "Carlos"),
                Map.entry("lastname", "Runner"),
                Map.entry("profile", "https://example.com/profile.png"),
                Map.entry("profile_medium", "https://example.com/profile-medium.png"),
                Map.entry("city", "Madrid"),
                Map.entry("country", "ES"),
                Map.entry("sex", "M"),
                Map.entry("measurement_preference", "meters"),
                Map.entry("created_at", "2026-01-01T10:00:00Z"),
                Map.entry("updated_at", "2026-04-10T11:30:00Z"),
                Map.entry("follower_count", 12),
                Map.entry("friend_count", 8),
                Map.entry("weight", 70.2),
                Map.entry("ftp", 295),
                Map.entry("summit", true),
                Map.entry("bikes", List.of(Map.of("id", "bike-1", "name", "Road Bike", "distance", 1234567)))
        ));
        when(stravaApiClient.fetchAthleteStats("access-token", 99L)).thenReturn(Map.ofEntries(
                Map.entry("all_run_totals", Map.of(
                        "distance", 120000.0,
                        "count", 20,
                        "moving_time", 24000,
                        "elapsed_time", 25000,
                        "elevation_gain", 1200.0,
                        "achievement_count", 4
                )),
                Map.entry("all_ride_totals", Map.of(
                        "distance", 450000.0,
                        "count", 15,
                        "moving_time", 36000,
                        "elapsed_time", 37200,
                        "elevation_gain", 4800.0,
                        "achievement_count", 6
                )),
                Map.entry("all_swim_totals", Map.of(
                        "distance", 5000.0,
                        "count", 5,
                        "moving_time", 4200,
                        "elapsed_time", 4300,
                        "elevation_gain", 0.0,
                        "achievement_count", 0
                )),
                Map.entry("recent_run_totals", Map.of("distance", 15000.0, "count", 3)),
                Map.entry("ytd_run_totals", Map.of("distance", 50000.0, "count", 9)),
                Map.entry("biggest_ride_distance", 95000.0),
                Map.entry("biggest_climb_elevation_gain", 1400.0)
        ));

        EventBatch batch = collector.collect(connection, null);

        assertEquals(EventType.USER_METRICS, batch.eventType());
        assertEquals(1, batch.events().size());
        assertEquals("99", batch.events().get(0).normalizedPayload().get("providerAthleteId"));
        assertEquals("Carlos Runner", batch.events().get(0).normalizedPayload().get("displayName"));
        assertEquals("male", batch.events().get(0).normalizedPayload().get("gender"));
        assertEquals("metric", batch.events().get(0).normalizedPayload().get("measurementSystem"));
        assertEquals(575000.0, batch.events().get(0).normalizedPayload().get("lifetimeDistanceMeters"));
        assertEquals(295, batch.events().get(0).normalizedPayload().get("ftpWatts"));
        assertTrue(batch.events().get(0).eventId().startsWith("99:"));
        assertEquals(true, batch.events().get(0).normalizedPayload().get("subscriptionActive"));
        assertEquals("meters", batch.events().get(0).providerSpecificPayload().get("measurementPreference"));
    }
}
