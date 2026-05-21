package com.h2traindata.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProviderEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void serializesBaseAndSpecificFieldsAtEventRoot() throws Exception {
        ProviderEvent event = new ProviderEvent(
                "strava",
                "99",
                EventType.USER_STATE,
                "UserProfile",
                "99:UserProfile:1775822400000",
                Instant.parse("2026-04-10T12:00:00Z"),
                Map.of(
                        "timestamp", Instant.parse("2026-04-10T12:00:00Z"),
                        "sourceSystem", "strava",
                        "athleteId", "99",
                        "weight", 70.2,
                        "gender", "male"
                )
        );

        JsonNode json = objectMapper.valueToTree(event);

        assertEquals("99", json.get("athleteId").asText());
        assertEquals("strava", json.get("sourceSystem").asText());
        assertEquals("2026-04-10T12:00:00Z", json.get("timestamp").asText());
        assertEquals(70.2d, json.get("weight").asDouble());
        assertEquals("male", json.get("gender").asText());
        assertFalse(json.has("normalizedPayload"));
        assertFalse(json.has("attributes"));
        assertFalse(json.has("providerId"));
        assertTrue(event.fields().containsKey("weight"));
        assertFalse(event.fields().containsKey("athleteId"));
    }
}
