package com.h2traindata.datalake.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h2traindata.bus.IncomingBusMessage;
import com.h2traindata.datalake.config.DatalakeProperties;
import com.h2traindata.datalake.io.DatalakeDeadLetterWriter;
import com.h2traindata.datalake.io.DatalakeEventParser;
import com.h2traindata.datalake.io.DatalakeEventWriter;
import com.h2traindata.datalake.io.DatalakePathResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatalakeIngestionServiceTest {

    @TempDir
    private Path tempDir;

    private DatalakeIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        DatalakeProperties properties = new DatalakeProperties();
        properties.setRootPath(tempDir);
        DatalakePathResolver pathResolver = new DatalakePathResolver(properties);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ingestionService = new DatalakeIngestionService(
                new DatalakeEventParser(objectMapper),
                new DatalakeEventWriter(pathResolver),
                new DatalakeDeadLetterWriter(pathResolver, objectMapper)
        );
    }

    @Test
    void writesValidBusMessageWithoutDependingOnKafka() throws Exception {
        String payload = """
                {
                  "messageId": "message-1",
                  "publishedAt": "2026-05-15T10:00:00Z",
                  "userId": "internal-user-1",
                  "providerId": "strava",
                  "eventType": "ACTIVITY",
                  "event": {
                    "timestamp": "2026-05-14T08:30:00Z"
                  }
                }
                """;

        DatalakeIngestionResult result = ingestionService.ingest(new IncomingBusMessage(
                "test-bus",
                "h2train.events.v1",
                null,
                null,
                "internal-user-1",
                payload
        ));

        Path expectedPath = tempDir.resolve(
                "events/strava/ACTIVITY/events.jsonl"
        );
        assertTrue(result.successful());
        assertEquals(expectedPath, result.file());
        assertEquals(Instant.parse("2026-05-14T08:30:00Z"), result.eventRecord().eventTimestamp());
        assertTrue(Files.exists(expectedPath));
    }

    @Test
    void writesInvalidBusMessageToDeadLetterWithoutDependingOnKafka() throws Exception {
        DatalakeIngestionResult result = ingestionService.ingest(new IncomingBusMessage(
                "test-bus",
                "h2train.events.v1",
                null,
                null,
                "bad-key",
                "not-json"
        ));

        String deadLetterLine = Files.readString(result.file());
        assertFalse(result.successful());
        assertTrue(result.file().toString().contains("dead-letter"));
        assertTrue(result.failureReason().contains("not valid JSON"));
        assertTrue(deadLetterLine.contains("\"source\":\"test-bus\""));
        assertTrue(deadLetterLine.contains("\"channel\":\"h2train.events.v1\""));
        assertTrue(deadLetterLine.contains("\"rawPayload\":\"not-json\""));
    }
}
