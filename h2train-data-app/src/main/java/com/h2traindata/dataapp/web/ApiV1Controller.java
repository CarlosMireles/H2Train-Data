package com.h2traindata.dataapp.web;

import com.h2traindata.dataapp.application.TimeSeriesQueryService;
import com.h2traindata.dataapp.domain.ActivityRecord;
import com.h2traindata.dataapp.domain.DatasetMetadata;
import com.h2traindata.dataapp.domain.PageResult;
import com.h2traindata.dataapp.domain.ProviderSyncStatus;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.SyncHistoryEntry;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import com.h2traindata.dataapp.web.ApiV1Dtos.ActivityResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.DailySummaryResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.DataCoverageResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.DatasetExportResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.DatasetMetadataResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.HealthResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.MetricSeriesResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.PageResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.ProviderResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.SyncHistoryResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.TimeSeriesBatchResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.TimeSeriesPointResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.TimeSeriesResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.UserResponse;
import com.h2traindata.dataapp.web.ApiV1Dtos.WeeklySummaryResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiV1Controller {

    private final TimeSeriesQueryService queryService;

    public ApiV1Controller(TimeSeriesQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/users/{userId}")
    public UserResponse user(@PathVariable String userId) {
        return toUser(queryService.user(userId));
    }

    @GetMapping("/users/{userId}/providers")
    public List<ProviderResponse> providers(@PathVariable String userId) {
        return queryService.providers(userId).stream()
                .map(this::toProvider)
                .toList();
    }

    @GetMapping("/users/{userId}/metrics")
    public List<String> metrics(@PathVariable String userId) {
        return queryService.metrics(userId);
    }

    @GetMapping("/users/{userId}/timeseries")
    public TimeSeriesResponse timeSeries(@PathVariable String userId,
                                         @RequestParam String metric,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) String resolution) {
        String normalizedMetric = normalizeMetric(metric);
        List<TimeSeriesPoint> points = queryService.timeSeries(userId, normalizedMetric, from, to);
        return new TimeSeriesResponse(
                userId,
                normalizedMetric,
                unit(points),
                resolution,
                from,
                to,
                points.stream().map(this::toPoint).toList()
        );
    }

    @GetMapping("/users/{userId}/timeseries/batch")
    public TimeSeriesBatchResponse timeSeriesBatch(@PathVariable String userId,
                                                   @RequestParam String metrics,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Map<String, List<TimeSeriesPoint>> series = queryService.timeSeriesBatch(
                userId,
                splitMetrics(metrics),
                from,
                to
        );
        return new TimeSeriesBatchResponse(
                userId,
                from,
                to,
                series.entrySet().stream()
                        .map(entry -> new MetricSeriesResponse(
                                entry.getKey(),
                                unit(entry.getValue()),
                                entry.getValue().stream().map(this::toPoint).toList()
                        ))
                        .toList()
        );
    }

    @GetMapping("/users/{userId}/summary/daily")
    public DailySummaryResponse dailySummary(@PathVariable String userId,
                                             @RequestParam
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TimeSeriesQueryService.DailySummary summary = queryService.dailySummary(userId, date);
        return new DailySummaryResponse(
                userId,
                summary.date(),
                summary.steps(),
                summary.distanceMeters(),
                summary.sleepDuration(),
                summary.calories(),
                summary.weight(),
                summary.activities()
        );
    }

    @GetMapping("/users/{userId}/summary/weekly")
    public WeeklySummaryResponse weeklySummary(@PathVariable String userId,
                                               @RequestParam
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        TimeSeriesQueryService.WeeklyActivitySummary summary =
                queryService.weeklyActivitySummary(userId, from, to);
        return new WeeklySummaryResponse(
                userId,
                summary.from(),
                summary.to(),
                summary.workoutCount(),
                summary.workoutDuration(),
                summary.distance(),
                summary.calories(),
                summary.durationBySport(),
                summary.distanceBySport(),
                summary.caloriesBySport()
        );
    }

    @GetMapping("/users/{userId}/activities")
    public PageResponse<ActivityResponse> activities(@PathVariable String userId,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(required = false) String activityType,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "50") int size) {
        PageResult<ActivityRecord> result = queryService.activities(userId, from, to, activityType, page, size);
        return new PageResponse<>(
                result.items().stream().map(this::toActivity).toList(),
                result.page(),
                result.size(),
                result.totalItems(),
                result.totalPages()
        );
    }

    @GetMapping("/users/{userId}/activities/{activityId}")
    public ActivityResponse activity(@PathVariable String userId, @PathVariable String activityId) {
        return toActivity(queryService.activity(userId, activityId));
    }

    @GetMapping("/users/{userId}/data-coverage")
    public Map<String, DataCoverageResponse> dataCoverage(@PathVariable String userId) {
        Map<String, DataCoverageResponse> response = new LinkedHashMap<>();
        queryService.dataCoverage(userId).forEach((metric, coverage) -> response.put(metric,
                new DataCoverageResponse(
                        coverage.firstRecord(),
                        coverage.lastRecord(),
                        coverage.daysWithData(),
                        coverage.missingDays(),
                        coverage.pointCount(),
                        coverage.period(),
                        coverage.unit()
                )));
        return response;
    }

    @GetMapping("/users/{userId}/sync/status")
    public List<ProviderResponse> syncStatus(@PathVariable String userId) {
        return queryService.syncStatus(userId).stream()
                .map(this::toProvider)
                .toList();
    }

    @GetMapping("/users/{userId}/sync/history")
    public List<SyncHistoryResponse> syncHistory(@PathVariable String userId) {
        return queryService.syncHistory(userId).stream()
                .map(this::toSyncHistory)
                .toList();
    }

    @GetMapping("/users/{userId}/dataset/export")
    public DatasetExportResponse datasetExport(@PathVariable String userId) {
        SubjectInfo subject = queryService.user(userId);
        Map<String, List<TimeSeriesPoint>> series = queryService.allTimeSeries(userId);
        Map<String, List<TimeSeriesPointResponse>> responseSeries = new LinkedHashMap<>();
        series.forEach((metric, points) -> responseSeries.put(metric, points.stream().map(this::toPoint).toList()));
        int pointCount = series.values().stream()
                .mapToInt(List::size)
                .sum();
        return new DatasetExportResponse(
                userId,
                toUser(subject),
                series.size(),
                pointCount,
                List.copyOf(series.keySet()),
                responseSeries
        );
    }

    @GetMapping("/dataset/metadata")
    public DatasetMetadataResponse datasetMetadata() {
        DatasetMetadata metadata = queryService.datasetMetadata();
        return new DatasetMetadataResponse(
                metadata.subjectCount(),
                metadata.metricCount(),
                metadata.pointCount(),
                metadata.firstRecord(),
                metadata.lastRecord(),
                metadata.metrics()
        );
    }

    @GetMapping("/dataset/subjects")
    public List<UserResponse> datasetSubjects() {
        return queryService.subjects().stream()
                .map(this::toUser)
                .toList();
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "h2train-data-app");
    }

    private UserResponse toUser(SubjectInfo subject) {
        return new UserResponse(
                subject.userId(),
                subject.providers().stream().sorted().toList(),
                subject.timezone(),
                subject.firstRecord(),
                subject.lastRecord()
        );
    }

    private ProviderResponse toProvider(ProviderSyncStatus status) {
        return new ProviderResponse(
                status.provider(),
                status.syncEnabled(),
                status.syncInterval(),
                status.lastSync(),
                status.status()
        );
    }

    private TimeSeriesPointResponse toPoint(TimeSeriesPoint point) {
        return new TimeSeriesPointResponse(
                point.periodStart(),
                point.periodEnd(),
                point.value(),
                point.unit(),
                point.period(),
                point.aggregationType() == null ? null : point.aggregationType().name(),
                point.provider(),
                point.activityType(),
                point.zone()
        );
    }

    private ActivityResponse toActivity(ActivityRecord activity) {
        return new ActivityResponse(
                activity.activityId(),
                activity.userId(),
                activity.provider(),
                activity.activityType(),
                activity.startTime(),
                activity.endTime(),
                activity.duration(),
                activity.distanceMeters(),
                activity.calories(),
                activity.updatedAt()
        );
    }

    private SyncHistoryResponse toSyncHistory(SyncHistoryEntry entry) {
        return new SyncHistoryResponse(
                entry.syncId(),
                entry.userId(),
                entry.provider(),
                entry.syncEnabled(),
                entry.syncInterval(),
                entry.syncedAt(),
                entry.status()
        );
    }

    private List<String> splitMetrics(String metrics) {
        if (!StringUtils.hasText(metrics)) {
            throw new IllegalArgumentException("metrics is required");
        }
        return List.of(metrics.split(",")).stream()
                .map(this::normalizeMetric)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String normalizeMetric(String metric) {
        if (!StringUtils.hasText(metric)) {
            throw new IllegalArgumentException("metric is required");
        }
        return metric.trim().toLowerCase(Locale.ROOT);
    }

    private String unit(List<TimeSeriesPoint> points) {
        return points.stream()
                .map(TimeSeriesPoint::unit)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
