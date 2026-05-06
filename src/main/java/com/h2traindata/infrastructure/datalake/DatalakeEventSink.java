package com.h2traindata.infrastructure.datalake;

import com.h2traindata.application.port.out.EventSink;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.ProviderEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import org.springframework.stereotype.Component;

@Component
public class DatalakeEventSink implements EventSink {

    private static final DateTimeFormatter BATCH_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    private final BronzePathResolver bronzePathResolver;
    private final DatalakeResourceResolver datalakeResourceResolver;
    private final SubjectIdResolver subjectIdResolver;
    private final com.h2traindata.infrastructure.config.DatalakeProperties datalakeProperties;
    private final JsonEventSerializer jsonEventSerializer;

    public DatalakeEventSink(BronzePathResolver bronzePathResolver,
                             DatalakeResourceResolver datalakeResourceResolver,
                             SubjectIdResolver subjectIdResolver,
                             com.h2traindata.infrastructure.config.DatalakeProperties datalakeProperties,
                             JsonEventSerializer jsonEventSerializer) {
        this.bronzePathResolver = bronzePathResolver;
        this.datalakeResourceResolver = datalakeResourceResolver;
        this.subjectIdResolver = subjectIdResolver;
        this.datalakeProperties = datalakeProperties;
        this.jsonEventSerializer = jsonEventSerializer;
    }

    @Override
    public void write(EventBatch batch) {
        if (batch.events().isEmpty()) {
            return;
        }

        Instant ingestedAt = Instant.now();
        String ingestionRunId = ingestionRunId(ingestedAt);
        String resource = datalakeResourceResolver.resolve(batch.eventType());
        String subjectId = subjectIdResolver.resolve(batch.providerId(), batch.athleteId());
        String batchId = batchId(batch, ingestedAt);
        Path outputPath = bronzePathResolver.resolve(batch, ingestedAt, batchId);

        try {
            Files.createDirectories(outputPath.getParent());
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new GZIPOutputStream(Files.newOutputStream(outputPath, StandardOpenOption.CREATE_NEW)),
                    StandardCharsets.UTF_8
            ))) {
                for (ProviderEvent event : batch.events()) {
                    writer.write(jsonEventSerializer.serialize(BronzeEventRecord.from(
                            batch,
                            event,
                            ingestedAt,
                            ingestionRunId,
                            batchId,
                            subjectId,
                            datalakeProperties.getDefaultConsentVersion(),
                            resource
                    )));
                    writer.newLine();
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to persist batch " + batchId + " to the datalake", exception);
        }
    }

    private String ingestionRunId(Instant ingestedAt) {
        return "ing-"
                + BATCH_TIMESTAMP_FORMATTER.format(ingestedAt)
                + "-"
                + UUID.randomUUID();
    }

    private String batchId(EventBatch batch, Instant ingestedAt) {
        String resource = datalakeResourceResolver.resolve(batch.eventType());
        return batch.providerId()
                + "-"
                + resource
                + "-"
                + BATCH_TIMESTAMP_FORMATTER.format(ingestedAt)
                + "-"
                + UUID.randomUUID();
    }
}
