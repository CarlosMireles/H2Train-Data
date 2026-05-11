package com.h2traindata.infrastructure.provider.strava.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StravaAthleteDto(
        Long id,
        String username,
        String firstname,
        String lastname
) {
}
