package com.h2traindata.infrastructure.datalake;

import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderEvent;
import java.time.Instant;
import java.util.Map;

public record BronzeEventRecord(
        int schemaVersion,
        String ingestionRunId,
        String batchId,
        String subjectId,
        String subjectScope,
        String consentVersion,
        String providerId,
        String resource,
        EventType eventType,
        String eventId,
        Instant occurredAt,
        Instant ingestedAt,
        int batchEventCount,
        String batchNextCursor,
        Map<String, Object> normalizedPayload,
        Map<String, Object> providerSpecificPayload,
        Map<String, Object> rawPayload
) {

    public static BronzeEventRecord from(EventBatch batch,
                                         ProviderEvent event,
                                         Instant ingestedAt,
                                         String ingestionRunId,
                                         String batchId,
                                         String subjectId,
                                         String consentVersion,
                                         String resource) {
        return new BronzeEventRecord(
                1,
                ingestionRunId,
                batchId,
                subjectId,
                "provider_account",
                consentVersion,
                event.providerId(),
                resource,
                event.eventType(),
                event.eventId(),
                event.occurredAt(),
                ingestedAt,
                batch.events().size(),
                batch.nextCursor() != null ? batch.nextCursor().value() : null,
                event.normalizedPayload(),
                event.providerSpecificPayload(),
                event.rawPayload()
        );
    }
}
