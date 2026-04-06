package com.h2traindata.web.dto;

public record ConnectAthleteResponse(
        String provider,
        String athleteId,
        String username,
        String eventType,
        int importedEvents,
        String message
) {
}
