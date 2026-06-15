package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.dataset.dto.DatasetQueryResponse;
import com.h2traindata.dataapp.dataset.format.DatasetFormat;

public record DatasetQueryResult(
        DatasetFormat format,
        DatasetQueryResponse response
) {
}
