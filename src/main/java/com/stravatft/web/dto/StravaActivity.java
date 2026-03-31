package com.stravatft.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record StravaActivity(
        long id,
        String name,
        String type,
        double distance,
        @JsonProperty("moving_time") int movingTime,
        @JsonProperty("elapsed_time") int elapsedTime,
        @JsonProperty("start_date") Instant startDate
) {
}
