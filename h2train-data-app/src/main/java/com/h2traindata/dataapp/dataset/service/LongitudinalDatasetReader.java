package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface LongitudinalDatasetReader {

    List<SubjectInfo> subjects();

    Set<String> availableMetrics();

    List<TimeSeriesPoint> readPoints(String userId, String metric, LocalDate from, LocalDate to);
}
