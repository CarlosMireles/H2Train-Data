package com.h2traindata.dataapp.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h2traindata.dataapp.config.DataAppDatalakeProperties;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalDatalakeEventRepositoryTest {

    @TempDir
    private Path tempDir;

    @Test
    void readsEventsFromDayNamedJsonlFiles() throws Exception {
        DataAppDatalakeProperties properties = new DataAppDatalakeProperties();
        properties.setRootPath(tempDir);
        Path eventFile = tempDir.resolve("events").resolve("ACTIVITY").resolve("2026-05-14.jsonl");
        Files.createDirectories(eventFile.getParent());
        Files.writeString(eventFile, """
                {"messageId":"message-1","publishedAt":"2026-05-15T10:00:00Z","userId":"internal-user-1","providerId":"strava","athleteId":"athlete-1","eventType":"ACTIVITY","eventName":"Workout","eventId":"workout-1","event":{"timestamp":"2026-05-14T08:30:00Z"}}
                """.strip(), StandardCharsets.UTF_8);

        LocalDatalakeEventRepository repository = new LocalDatalakeEventRepository(
                properties,
                new DatalakeEventJsonParser(new ObjectMapper().registerModule(new JavaTimeModule()))
        );

        List<NormalizedDatalakeEvent> events = repository.readEvents();

        assertEquals(1, events.size());
        assertEquals("internal-user-1", events.get(0).userId());
        assertEquals("ACTIVITY", events.get(0).eventType());
        assertEquals(Instant.parse("2026-05-14T08:30:00Z"), events.get(0).eventTimestamp());
    }
}
