package com.stravatft.web.dto;

public record ConnectAthleteResponse(
        long athleteId,
        String username,
        int importedActivities,
        String message
) {
}
