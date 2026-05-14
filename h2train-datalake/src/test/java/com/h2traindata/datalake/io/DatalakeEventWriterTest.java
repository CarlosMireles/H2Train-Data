package com.h2traindata.datalake.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.h2traindata.datalake.config.DatalakeProperties;
import com.h2traindata.datalake.domain.DatalakeEventRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatalakeEventWriterTest {

    @TempDir
    private Path tempDir;

    @Test
    void writesEventUsingUserProviderTypeAndEventDatePartitions() throws Exception {
        DatalakeProperties properties = new DatalakeProperties();
        properties.setRootPath(tempDir);
        DatalakePathResolver pathResolver = new DatalakePathResolver(properties);
        DatalakeEventWriter writer = new DatalakeEventWriter(pathResolver);
        String rawJson = """
                {"messageId":"message-1","userId":"internal-user-1","providerId":"strava","eventType":"ACTIVITY"}
                """.strip();

        Path target = writer.write(new DatalakeEventRecord(
                rawJson,
                "internal-user-1",
                "strava",
                "ACTIVITY",
                Instant.parse("2026-05-14T08:30:00Z")
        ));

        Path expectedPath = tempDir.resolve(
                "events/userId=internal-user-1/provider=strava/eventType=ACTIVITY/year=2026/month=05/day=14/events.jsonl"
        );
        assertEquals(expectedPath, target);
        assertTrue(Files.exists(expectedPath));
        assertEquals(rawJson, Files.readString(expectedPath).strip());
    }
}
