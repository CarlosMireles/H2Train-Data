package com.h2traindata.infrastructure.provider.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FitbitStepsResponseDto(
        @JsonProperty("activities-steps") List<FitbitStepsSummaryEntryDto> activitiesSteps,
        @JsonProperty("activities-steps-intraday") FitbitIntradayDto activitiesStepsIntraday
) {
}
