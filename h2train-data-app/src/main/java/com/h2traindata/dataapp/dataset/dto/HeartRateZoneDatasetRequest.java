package com.h2traindata.dataapp.dataset.dto;

import java.time.LocalDate;
import java.util.List;

public record HeartRateZoneDatasetRequest(
        LocalDate from,
        LocalDate to,
        List<String> zones,
        List<String> providers,
        String format
) {
}
