package com.h2traindata.dataapp.dataset.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DatasetExportRequest(
        List<String> metrics,
        List<DatasetFilterRequest> filters,
        Map<String, List<String>> dimensions,
        LocalDate from,
        LocalDate to,
        String format
) {
}
