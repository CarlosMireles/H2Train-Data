package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.dataset.dto.DatasetExportResponse;
import com.h2traindata.dataapp.dataset.format.DatasetFormat;

public record DatasetExportResult(
        DatasetFormat format,
        DatasetExportResponse response
) {
}
