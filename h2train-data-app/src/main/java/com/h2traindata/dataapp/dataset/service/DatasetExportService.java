package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.dataset.aggregation.DatasetAggregationService;
import com.h2traindata.dataapp.dataset.dto.DatasetExportRequest;
import com.h2traindata.dataapp.dataset.dto.DatasetExportResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetExportRow;
import com.h2traindata.dataapp.dataset.filter.DatasetFilter;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DatasetExportService {

    private final LongitudinalDatasetReader reader;
    private final DatasetRequestValidator validator;
    private final DatasetAggregationService aggregationService;

    public DatasetExportService(LongitudinalDatasetReader reader,
                                DatasetRequestValidator validator,
                                DatasetAggregationService aggregationService) {
        this.reader = reader;
        this.validator = validator;
        this.aggregationService = aggregationService;
    }

    public DatasetExportResult export(DatasetExportRequest request) {
        DatasetRequestValidator.ValidatedExport export = validator.validate(request);
        List<DatasetExportRow> rows = new ArrayList<>();
        int matchedSubjects = 0;
        for (SubjectInfo subject : reader.subjects().stream()
                .sorted(Comparator.comparing(SubjectInfo::userId))
                .toList()) {
            Map<String, List<TimeSeriesPoint>> pointsByMetric = new LinkedHashMap<>();
            if (!matchesAllFilters(subject.userId(), export, pointsByMetric)) {
                continue;
            }
            matchedSubjects++;
            for (String metric : export.metrics()) {
                export.dimensions().filter(points(
                                subject.userId(),
                                metric,
                                export,
                                pointsByMetric
                        )).stream()
                        .map(this::toRow)
                        .forEach(rows::add);
            }
        }
        rows.sort(Comparator
                .comparing(DatasetExportRow::userId)
                .thenComparing(DatasetExportRow::metric)
                .thenComparing(DatasetExportRow::periodStart, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(row -> nullToEmpty(row.activityType()))
                .thenComparing(row -> nullToEmpty(row.zone())));
        DatasetExportResponse response = new DatasetExportResponse(
                export.normalizedRequest(),
                matchedSubjects,
                rows.size(),
                List.copyOf(rows)
        );
        return new DatasetExportResult(export.format(), response);
    }

    private boolean matchesAllFilters(String userId,
                                      DatasetRequestValidator.ValidatedExport export,
                                      Map<String, List<TimeSeriesPoint>> pointsByMetric) {
        for (DatasetFilter filter : export.filters()) {
            boolean matches = aggregationService.aggregate(
                            filter.aggregation(),
                            filter.dimensions().filter(points(
                                    userId,
                                    filter.metric(),
                                    export,
                                    pointsByMetric
                            )))
                    .filter(filter::matches)
                    .isPresent();
            if (!matches) {
                return false;
            }
        }
        return true;
    }

    private List<TimeSeriesPoint> points(String userId,
                                         String metric,
                                         DatasetRequestValidator.ValidatedExport export,
                                         Map<String, List<TimeSeriesPoint>> pointsByMetric) {
        return pointsByMetric.computeIfAbsent(metric,
                ignored -> reader.readPoints(userId, metric, export.from(), export.to()));
    }

    private DatasetExportRow toRow(TimeSeriesPoint point) {
        return new DatasetExportRow(
                point.userId(),
                point.metricName(),
                point.value(),
                point.periodStart(),
                point.periodEnd(),
                point.unit(),
                point.period(),
                point.provider(),
                point.activityType(),
                point.zone()
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
