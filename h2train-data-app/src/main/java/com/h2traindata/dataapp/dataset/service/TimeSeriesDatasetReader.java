package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.application.TimeSeriesQueryService;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TimeSeriesDatasetReader implements LongitudinalDatasetReader {

    private final TimeSeriesQueryService queryService;

    public TimeSeriesDatasetReader(TimeSeriesQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public List<SubjectInfo> subjects() {
        return queryService.subjects();
    }

    @Override
    public Set<String> availableMetrics() {
        Set<String> metrics = new LinkedHashSet<>();
        subjects().forEach(subject -> metrics.addAll(queryService.metrics(subject.userId())));
        return Set.copyOf(metrics);
    }

    @Override
    public List<TimeSeriesPoint> readPoints(String userId, String metric, LocalDate from, LocalDate to) {
        return queryService.timeSeries(userId, metric, from, to);
    }
}
