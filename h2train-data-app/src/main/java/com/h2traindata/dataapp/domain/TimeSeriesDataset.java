package com.h2traindata.dataapp.domain;

import java.util.List;

public record TimeSeriesDataset(
        List<SubjectInfo> subjects,
        List<TimeSeriesPoint> points
) {
}
