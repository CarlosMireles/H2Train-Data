package com.h2traindata.web.dto;

public record SyncEventsResponse(
        String provider,
        String athleteId,
        String eventType,
        int importedEvents,
        String message
) {
}
