package com.h2traindata.dataapp.dataset.dto;

import java.util.List;

public record HeartRateZoneDatasetResponse(
        HeartRateZoneDatasetRequest query,
        List<HeartRateZoneDay> days
) {
}
