package com.h2traindata.dataapp.dataset.dto;

import java.util.List;

public record DatasetQueryResponse(
        DatasetQueryRequest query,
        List<DatasetSubjectMatch> subjects
) {
}
