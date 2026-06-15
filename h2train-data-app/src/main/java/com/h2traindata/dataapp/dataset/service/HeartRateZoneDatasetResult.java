package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetResponse;
import com.h2traindata.dataapp.dataset.format.DatasetFormat;

public record HeartRateZoneDatasetResult(
        DatasetFormat format,
        HeartRateZoneDatasetResponse response
) {
}
