package com.h2traindata.dataapp.dataset.dto;

import java.util.List;

public record DatasetCapabilitiesResponse(
        List<String> metrics,
        List<String> operators,
        List<String> aggregations,
        List<String> formats,
        List<String> dimensions
) {
}
