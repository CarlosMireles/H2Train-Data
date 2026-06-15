package com.h2traindata.dataapp.dataset.dto;

import java.util.List;

public record DatasetExportResponse(
        DatasetExportRequest request,
        int subjectCount,
        int rowCount,
        List<DatasetExportRow> rows
) {
}
