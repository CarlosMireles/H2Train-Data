package com.h2traindata.datalake.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h2traindata.datalake.domain.DatalakeEventRecord;
import com.h2traindata.datalake.domain.InvalidDatalakeEventException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DatalakeEventParserTest {

    private final DatalakeEventParser parser = new DatalakeEventParser(
            new ObjectMapper().registerModule(new JavaTimeModule())
    );

    @Test
    void extractsPartitionFieldsFromBusEnvelopeJson() {
        DatalakeEventRecord eventRecord = parser.parse("""
                {
                  "messageId": "message-1",
                  "publishedAt": "2026-05-15T10:00:00Z",
                  "userId": "internal-user-1",
                  "providerId": "fitbit",
                  "eventType": "HEALTH",
                  "event": {
                    "timestamp": "2026-05-14T21:30:00Z"
                  }
                }
                """);

        assertEquals("internal-user-1", eventRecord.userId());
        assertEquals("fitbit", eventRecord.providerId());
        assertEquals("HEALTH", eventRecord.eventType());
        assertEquals(Instant.parse("2026-05-14T21:30:00Z"), eventRecord.eventTimestamp());
    }

    @Test
    void rejectsInvalidJson() {
        assertThrows(InvalidDatalakeEventException.class, () -> parser.parse("not-json"));
    }

    @Test
    void acceptsBusEnvelopeJsonWithByteOrderMark() {
        DatalakeEventRecord eventRecord = parser.parse("\uFEFF" + """
                {
                  "messageId": "message-1",
                  "publishedAt": "2026-05-15T10:00:00Z",
                  "userId": "internal-user-1",
                  "providerId": "strava",
                  "eventType": "ACTIVITY",
                  "event": {
                    "timestamp": "2026-05-14T21:30:00Z"
                  }
                }
                """);

        assertEquals("internal-user-1", eventRecord.userId());
        assertTrue(eventRecord.rawJson().startsWith("{"));
    }

    @Test
    void rejectsJsonThatIsNotBusEnvelope() {
        InvalidDatalakeEventException exception = assertThrows(
                InvalidDatalakeEventException.class,
                () -> parser.parse("""
                        {"type":"kafka-smoke-test","status":"ok"}
                        """)
        );

        assertTrue(exception.getMessage().contains("missing userId"));
    }
}
