package com.h2traindata.datalake.io;

import com.h2traindata.datalake.config.DatalakeProperties;
import com.h2traindata.datalake.domain.DatalakeEventRecord;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

@Component
public class DatalakePathResolver {

    private final DatalakeProperties properties;

    public DatalakePathResolver(DatalakeProperties properties) {
        this.properties = properties;
    }

    public Path eventsFile(DatalakeEventRecord eventRecord) {
        return properties.getRootPath()
                .resolve("events")
                .resolve(segment(eventRecord.eventType()))
                .resolve(eventFileName(eventRecord));
    }

    public Path deadLetterFile(ZonedDateTime failedAt) {
        ZonedDateTime timestamp = failedAt.withZoneSameInstant(ZoneOffset.UTC);
        return properties.getRootPath()
                .resolve("dead-letter")
                .resolve("year=%04d".formatted(timestamp.getYear()))
                .resolve("month=%02d".formatted(timestamp.getMonthValue()))
                .resolve("day=%02d".formatted(timestamp.getDayOfMonth()))
                .resolve("failed-events.jsonl");
    }

    private String segment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String eventFileName(DatalakeEventRecord eventRecord) {
        Instant timestamp = eventRecord.eventTimestamp() != null
                ? eventRecord.eventTimestamp()
                : Instant.now();
        return timestamp.atZone(ZoneOffset.UTC).toLocalDate() + ".jsonl";
    }
}
