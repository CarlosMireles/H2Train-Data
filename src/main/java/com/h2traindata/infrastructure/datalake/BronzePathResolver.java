package com.h2traindata.infrastructure.datalake;

import com.h2traindata.domain.EventBatch;
import com.h2traindata.infrastructure.config.DatalakeProperties;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class BronzePathResolver {

    private static final DateTimeFormatter INGEST_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DatalakeProperties datalakeProperties;
    private final DatalakeResourceResolver datalakeResourceResolver;

    public BronzePathResolver(DatalakeProperties datalakeProperties,
                              DatalakeResourceResolver datalakeResourceResolver) {
        this.datalakeProperties = datalakeProperties;
        this.datalakeResourceResolver = datalakeResourceResolver;
    }

    public Path resolve(EventBatch batch, Instant ingestedAt, String batchId) {
        String resource = datalakeResourceResolver.resolve(batch.eventType());
        return datalakeProperties.rootDirectory()
                .resolve("bronze")
                .resolve("zone=restricted")
                .resolve("provider=" + batch.providerId())
                .resolve("resource=" + resource)
                .resolve("ingest_date=" + INGEST_DATE_FORMATTER.format(ingestedAt.atZone(ZoneOffset.UTC)))
                .resolve(batchId + ".jsonl.gz");
    }
}
