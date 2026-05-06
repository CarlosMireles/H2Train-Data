package com.h2traindata.infrastructure.datalake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.infrastructure.config.DatalakeProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatalakeEventSinkTest {

    @TempDir
    Path tempDir;

    @Test
    void writesCompressedJsonLinesIntoBronzeLayout() throws IOException {
        DatalakeProperties datalakeProperties = new DatalakeProperties();
        datalakeProperties.setRootPath(tempDir.resolve("lake").toString());
        datalakeProperties.setSubjectIdSalt("test-salt");
        datalakeProperties.setDefaultConsentVersion("study-v3");

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        DatalakeResourceResolver resourceResolver = new DatalakeResourceResolver();
        DatalakeEventSink eventSink = new DatalakeEventSink(
                new BronzePathResolver(datalakeProperties, resourceResolver),
                resourceResolver,
                new SubjectIdResolver(datalakeProperties),
                datalakeProperties,
                new JsonEventSerializer(objectMapper)
        );

        EventBatch batch = new EventBatch(
                "strava",
                "7",
                EventType.ACTIVITY,
                List.of(new ProviderEvent(
                        "strava",
                        "7",
                        EventType.ACTIVITY,
                        "123",
                        Instant.parse("2026-04-03T10:15:30Z"),
                        Map.of("name", "Morning Ride"),
                        Map.of("kudosCount", 3),
                        Map.of("id", 123L)
                )),
                new SyncCursor("1700003600")
        );

        eventSink.write(batch);

        List<Path> files;
        try (Stream<Path> pathStream = Files.walk(datalakeProperties.rootDirectory().resolve("bronze"))) {
            files = pathStream.filter(Files::isRegularFile).toList();
        }

        assertEquals(1, files.size());
        Path outputFile = files.get(0);
        assertTrue(outputFile.toString().contains("zone=restricted"));
        assertTrue(outputFile.toString().contains("provider=strava"));
        assertTrue(outputFile.toString().contains("resource=activity"));
        assertTrue(outputFile.getFileName().toString().endsWith(".jsonl.gz"));
        assertTrue(!outputFile.getFileName().toString().contains("-7-"));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(outputFile)),
                StandardCharsets.UTF_8
        ))) {
            String line = reader.readLine();
            assertNotNull(line);

            JsonNode payload = objectMapper.readTree(line);
            assertEquals("strava", payload.get("providerId").asText());
            assertEquals("activity", payload.get("resource").asText());
            assertEquals("ACTIVITY", payload.get("eventType").asText());
            assertEquals("123", payload.get("eventId").asText());
            assertEquals("1700003600", payload.get("batchNextCursor").asText());
            assertEquals("provider_account", payload.get("subjectScope").asText());
            assertEquals("study-v3", payload.get("consentVersion").asText());
            assertTrue(payload.get("subjectId").asText().startsWith("subj_"));
            assertNotNull(payload.get("ingestionRunId").asText());
            assertEquals("Morning Ride", payload.get("normalizedPayload").get("name").asText());
        }
    }

    @Test
    void doesNotCreateFilesForEmptyBatches() throws IOException {
        DatalakeProperties datalakeProperties = new DatalakeProperties();
        datalakeProperties.setRootPath(tempDir.resolve("lake-empty").toString());

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        DatalakeResourceResolver resourceResolver = new DatalakeResourceResolver();
        DatalakeEventSink eventSink = new DatalakeEventSink(
                new BronzePathResolver(datalakeProperties, resourceResolver),
                resourceResolver,
                new SubjectIdResolver(datalakeProperties),
                datalakeProperties,
                new JsonEventSerializer(objectMapper)
        );

        EventBatch batch = new EventBatch(
                "strava",
                "7",
                EventType.ACTIVITY,
                List.of(),
                new SyncCursor("1700003600")
        );

        eventSink.write(batch);

        assertTrue(Files.notExists(datalakeProperties.rootDirectory().resolve("bronze")));
    }
}
