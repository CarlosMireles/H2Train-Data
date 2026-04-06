package com.h2traindata.infrastructure.provider.fitbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.infrastructure.provider.fitbit.client.FitbitApiClient;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FitbitActivityCollectorTest {

    private final FitbitApiClient fitbitApiClient = Mockito.mock(FitbitApiClient.class);
    private final FitbitActivityCollector collector = new FitbitActivityCollector(fitbitApiClient);

    @Test
    void mapsActivityLogsToNormalizedActivityEvents() {
        ProviderConnection connection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("ABC123", "fitbit-runner"),
                "fitbit-access-token",
                "fitbit-refresh-token",
                Instant.now().plusSeconds(600)
        );
        when(fitbitApiClient.fetchActivityLogs("fitbit-access-token", null, LocalDate.now().toString(), 50)).thenReturn(List.of(Map.ofEntries(
                Map.entry("logId", 19018673358L),
                Map.entry("activityName", "Walk"),
                Map.entry("activityTypeId", 90013),
                Map.entry("averageHeartRate", 126),
                Map.entry("calories", 204),
                Map.entry("distance", 2.4),
                Map.entry("distanceUnit", "Kilometers"),
                Map.entry("activeDuration", 1_500_000L),
                Map.entry("duration", 1_536_000L),
                Map.entry("steps", 1799),
                Map.entry("elevationGain", 0),
                Map.entry("logType", "auto_detected"),
                Map.entry("manualValuesSpecified", Map.of("calories", false, "distance", false, "steps", false)),
                Map.entry("startTime", "2026-04-03T12:08:29.000+02:00"),
                Map.entry("source", Map.of("name", "Pixel Watch")),
                Map.entry("tcxLink", "https://api.fitbit.com/1/user/-/activities/19018673358.tcx")
        )));
        when(fitbitApiClient.fetchActivityTcx("fitbit-access-token", 19018673358L)).thenReturn("""
                <?xml version="1.0" encoding="UTF-8"?>
                <TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2">
                  <Activities>
                    <Activity Sport="Walking">
                      <Lap StartTime="2026-04-03T10:08:29Z">
                        <TotalTimeSeconds>1536</TotalTimeSeconds>
                        <DistanceMeters>2400.0</DistanceMeters>
                        <Calories>204</Calories>
                        <Track>
                          <Trackpoint>
                            <Time>2026-04-03T10:08:29Z</Time>
                            <Position>
                              <LatitudeDegrees>40.4168</LatitudeDegrees>
                              <LongitudeDegrees>-3.7038</LongitudeDegrees>
                            </Position>
                            <AltitudeMeters>650.0</AltitudeMeters>
                            <DistanceMeters>0.0</DistanceMeters>
                            <HeartRateBpm><Value>120</Value></HeartRateBpm>
                          </Trackpoint>
                          <Trackpoint>
                            <Time>2026-04-03T10:33:29Z</Time>
                            <Position>
                              <LatitudeDegrees>40.4172</LatitudeDegrees>
                              <LongitudeDegrees>-3.7041</LongitudeDegrees>
                            </Position>
                            <AltitudeMeters>655.0</AltitudeMeters>
                            <DistanceMeters>2400.0</DistanceMeters>
                            <HeartRateBpm><Value>132</Value></HeartRateBpm>
                          </Trackpoint>
                        </Track>
                      </Lap>
                    </Activity>
                  </Activities>
                </TrainingCenterDatabase>
                """);
        when(fitbitApiClient.fetchWorkoutSummary("fitbit-access-token", 19018673358L)).thenReturn(Map.of(
                "workoutSummary", Map.of(
                        "logId", 19018673358L,
                        "mobilityMetrics", Map.of(
                                "avgCadenceStepsPerMinute", 118,
                                "avgGroundContactTimeMilliseconds", 345
                        )
                )
        ));

        EventBatch batch = collector.collect(connection, null);

        assertEquals(EventType.ACTIVITY, batch.eventType());
        assertEquals("fitbit", batch.providerId());
        assertEquals(1, batch.events().size());
        assertEquals("19018673358", batch.events().get(0).eventId());
        assertEquals(2400.0, batch.events().get(0).normalizedPayload().get("distanceMeters"));
        assertEquals(118.0, batch.events().get(0).normalizedPayload().get("averageCadenceRpm"));
        assertEquals(132.0, batch.events().get(0).normalizedPayload().get("maxHeartRateBpm"));
        assertEquals("auto_detected", batch.events().get(0).providerSpecificPayload().get("logType"));
        assertInstanceOf(String.class, batch.events().get(0).rawPayload().get("tcxXml"));
    }
}
