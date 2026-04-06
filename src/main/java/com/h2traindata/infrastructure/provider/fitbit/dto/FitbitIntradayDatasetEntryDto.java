package com.h2traindata.infrastructure.provider.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FitbitIntradayDatasetEntryDto(
        String time,
        int value
) {
}
