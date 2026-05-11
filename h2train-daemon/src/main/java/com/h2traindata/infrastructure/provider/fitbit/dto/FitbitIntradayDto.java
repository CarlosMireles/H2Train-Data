package com.h2traindata.infrastructure.provider.fitbit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FitbitIntradayDto(
        List<FitbitIntradayDatasetEntryDto> dataset,
        int datasetInterval,
        String datasetType
) {
}
