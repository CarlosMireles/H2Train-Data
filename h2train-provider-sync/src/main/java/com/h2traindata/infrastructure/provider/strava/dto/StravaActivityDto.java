package com.h2traindata.infrastructure.provider.strava.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StravaActivityDto(
        long id,
        String name,
        String type,
        double distance,
        @JsonProperty("moving_time") int movingTime,
        @JsonProperty("elapsed_time") int elapsedTime,
        @JsonProperty("start_date") Instant startDate
) {
}
