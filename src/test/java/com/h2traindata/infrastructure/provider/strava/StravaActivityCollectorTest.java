package com.h2traindata.infrastructure.provider.strava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

class StravaActivityCollectorTest {

    private final StravaApiClient stravaApiClient = Mockito.mock(StravaApiClient.class);
    private final StravaActivityCollector collector = new StravaActivityCollector(
            stravaApiClient,
            command -> command.run()
    );

    @Test
    void mapsDetailedActivitiesToRichProviderEvents() {
        ProviderConnection connection = new ProviderConnection(
                "strava",
                new AthleteProfile("99", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600)
        );

        when(stravaApiClient.fetchActivities("access-token", 50, null)).thenReturn(List.of(Map.of(
                "id", 321L,
                "start_date", "2026-04-03T10:15:30Z"
        )));
        when(stravaApiClient.fetchActivity("access-token", 321L)).thenReturn(Map.ofEntries(
                Map.entry("id", 321L),
                Map.entry("external_id", "garmin-fit-321"),
                Map.entry("name", "Lunch Run"),
                Map.entry("description", "Tempo session"),
                Map.entry("sport_type", "Run"),
                Map.entry("type", "Run"),
                Map.entry("distance", 10_000.0),
                Map.entry("moving_time", 1800),
                Map.entry("elapsed_time", 1900),
                Map.entry("start_date", "2026-04-03T10:15:30Z"),
                Map.entry("start_date_local", "2026-04-03T12:15:30+02:00"),
                Map.entry("timezone", "(GMT+01:00) Europe/Madrid"),
                Map.entry("total_elevation_gain", 120.0),
                Map.entry("average_speed", 5.55),
                Map.entry("max_speed", 6.21),
                Map.entry("average_heartrate", 154.2),
                Map.entry("max_heartrate", 172),
                Map.entry("average_cadence", 84.0),
                Map.entry("average_watts", 245.0),
                Map.entry("max_watts", 410),
                Map.entry("device_name", "Garmin Forerunner"),
                Map.entry("trainer", false),
                Map.entry("commute", false),
                Map.entry("manual", false),
                Map.entry("private", false),
                Map.entry("gear_id", "g1"),
                Map.entry("start_latlng", List.of(40.4168, -3.7038)),
                Map.entry("end_latlng", List.of(40.4172, -3.7041)),
                Map.entry("map", Map.of("polyline", "abcd", "summary_polyline", "xyz")),
                Map.entry("achievement_count", 2),
                Map.entry("kudos_count", 5),
                Map.entry("splits_metric", List.of(Map.of(
                        "split", 1,
                        "distance", 1000.0,
                        "elapsed_time", 240,
                        "moving_time", 235,
                        "average_speed", 4.2,
                        "elevation_difference", 5.0,
                        "pace_zone", 2
                ))),
                Map.entry("laps", List.of(Map.of(
                        "id", 1L,
                        "name", "Lap 1",
                        "start_date", "2026-04-03T10:15:30Z",
                        "elapsed_time", 900,
                        "moving_time", 880,
                        "distance", 5000.0,
                        "average_speed", 5.6
                )))
        ));
        when(stravaApiClient.fetchActivityStreams("access-token", 321L)).thenReturn(Map.of(
                "time", Map.of("data", List.of(0, 60)),
                "distance", Map.of("data", List.of(0.0, 350.0)),
                "latlng", Map.of("data", List.of(List.of(40.4168, -3.7038), List.of(40.4170, -3.7039))),
                "altitude", Map.of("data", List.of(650.0, 655.0)),
                "heartrate", Map.of("data", List.of(148, 160)),
                "moving", Map.of("data", List.of(true, true))
        ));
        when(stravaApiClient.fetchActivityZones("access-token", 321L)).thenReturn(List.of(Map.of(
                "type", "heartrate",
                "score", 12
        )));

        EventBatch batch = collector.collect(connection, null);

        assertEquals(EventType.ACTIVITY, batch.eventType());
        assertEquals(1, batch.events().size());
        assertEquals("321", batch.events().get(0).eventId());
        assertEquals("Lunch Run", batch.events().get(0).normalizedPayload().get("name"));
        assertEquals(10_000.0, batch.events().get(0).normalizedPayload().get("distanceMeters"));
        assertEquals("garmin-fit-321", batch.events().get(0).normalizedPayload().get("externalId"));
        assertEquals(2, ((List<?>) batch.events().get(0).normalizedPayload().get("trackpoints")).size());
        assertEquals(5, batch.events().get(0).providerSpecificPayload().get("kudosCount"));
        assertInstanceOf(Map.class, batch.events().get(0).rawPayload().get("detailedActivity"));
    }
}
