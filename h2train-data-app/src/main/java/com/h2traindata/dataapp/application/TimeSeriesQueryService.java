package com.h2traindata.dataapp.application;

import com.h2traindata.dataapp.domain.ActivityRecord;
import com.h2traindata.dataapp.domain.DatasetExport;
import com.h2traindata.dataapp.domain.DatasetMetadata;
import com.h2traindata.dataapp.domain.PageResult;
import com.h2traindata.dataapp.domain.ProviderSyncStatus;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.SyncHistoryEntry;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import com.h2traindata.dataapp.infrastructure.LongitudinalDatamartRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TimeSeriesQueryService {

    private static final Set<String> WEEKLY_ACTIVITY_METRICS = Set.of(
            "weekly_activity_count",
            "weekly_workout_duration",
            "weekly_workout_distance_by_sport",
            "weekly_workout_calories_by_sport"
    );

    private final LongitudinalDatamartRepository datamartRepository;

    public TimeSeriesQueryService(LongitudinalDatamartRepository datamartRepository) {
        this.datamartRepository = datamartRepository;
    }

    public SubjectInfo user(String userId) {
        requireUserId(userId);
        return datamartRepository.findSubject(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
    }

    public List<SubjectInfo> subjects() {
        return datamartRepository.listSubjects();
    }

    public DatasetMetadata datasetMetadata() {
        return datamartRepository.datasetMetadata();
    }

    public List<ProviderSyncStatus> providers(String userId) {
        requireUserId(userId);
        Map<String, ProviderSyncStatus> providers = new LinkedHashMap<>();
        datamartRepository.readSyncStatus(userId).forEach(status -> {
            if (StringUtils.hasText(status.provider())) {
                providers.put(status.provider(), status);
            }
        });
        datamartRepository.findSubject(userId)
                .map(SubjectInfo::providers)
                .orElse(Set.of())
                .stream()
                .sorted()
                .forEach(provider -> providers.putIfAbsent(provider,
                        new ProviderSyncStatus(provider, null, null, null, null)));
        return providers.values().stream()
                .sorted(Comparator.comparing(status -> nullToEmpty(status.provider())))
                .toList();
    }

    public List<String> metrics(String userId) {
        requireUserId(userId);
        return datamartRepository.metrics(userId);
    }

    public List<TimeSeriesPoint> timeSeries(String userId, String metric, LocalDate from, LocalDate to) {
        requireUserId(userId);
        if (!StringUtils.hasText(metric)) {
            throw new IllegalArgumentException("metric is required");
        }
        validateRange(from, to);
        ZoneId zone = zoneForSubject(userId);
        String normalizedMetric = metric.trim().toLowerCase(Locale.ROOT);
        return datamartRepository.readPoints(userId, normalizedMetric).stream()
                .filter(point -> withinRange(point, zone, from, to))
                .sorted(pointComparator())
                .toList();
    }

    public Map<String, List<TimeSeriesPoint>> timeSeriesBatch(String userId, List<String> requestedMetrics,
                                                              LocalDate from, LocalDate to) {
        requireUserId(userId);
        if (requestedMetrics == null || requestedMetrics.stream().noneMatch(StringUtils::hasText)) {
            throw new IllegalArgumentException("metrics is required");
        }
        validateRange(from, to);
        Map<String, List<TimeSeriesPoint>> series = new LinkedHashMap<>();
        requestedMetrics.stream()
                .filter(StringUtils::hasText)
                .map(metric -> metric.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .forEach(metric -> series.put(metric, timeSeries(userId, metric, from, to)));
        return series;
    }

    public List<TimeSeriesPoint> weeklySummary(String userId, LocalDate from, LocalDate to) {
        requireUserId(userId);
        validateRange(from, to);
        ZoneId zone = zoneForSubject(userId);
        return WEEKLY_ACTIVITY_METRICS.stream()
                .flatMap(metric -> datamartRepository.readPoints(userId, metric).stream())
                .filter(point -> withinRange(point, zone, from, to))
                .sorted(pointComparator())
                .toList();
    }

    public DailySummary dailySummary(String userId, LocalDate date) {
        requireUserId(userId);
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        return new DailySummary(
                date,
                sumOrNull(timeSeries(userId, "daily_steps", date, date)),
                sumOrNull(timeSeries(userId, "daily_distance", date, date)),
                sumOrNull(timeSeries(userId, "daily_sleep_duration", date, date)),
                sumOrNull(timeSeries(userId, "daily_calories", date, date)),
                lastOrNull(timeSeries(userId, "daily_weight", date, date)),
                sumOrNull(timeSeries(userId, "daily_activity_count", date, date))
        );
    }

    public WeeklyActivitySummary weeklyActivitySummary(String userId, LocalDate from, LocalDate to) {
        List<TimeSeriesPoint> points = weeklySummary(userId, from, to);
        return new WeeklyActivitySummary(
                from,
                to,
                sum(points, "weekly_activity_count"),
                sum(points, "weekly_workout_duration"),
                sum(points, "weekly_workout_distance_by_sport"),
                sum(points, "weekly_workout_calories_by_sport"),
                sumByActivityType(points, "weekly_workout_duration"),
                sumByActivityType(points, "weekly_workout_distance_by_sport"),
                sumByActivityType(points, "weekly_workout_calories_by_sport")
        );
    }

    public PageResult<ActivityRecord> activities(String userId, LocalDate from, LocalDate to, String activityType,
                                                 int page, int size) {
        requireUserId(userId);
        validateRange(from, to);
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 200);
        ZoneId zone = zoneForSubject(userId);
        String normalizedActivityType = StringUtils.hasText(activityType)
                ? activityType.trim().toLowerCase(Locale.ROOT)
                : null;
        List<ActivityRecord> filtered = datamartRepository.readActivities(userId).stream()
                .filter(activity -> activityWithinRange(activity, zone, from, to))
                .filter(activity -> normalizedActivityType == null
                        || normalizedActivityType.equals(nullToEmpty(activity.activityType()).toLowerCase(Locale.ROOT)))
                .toList();
        long totalItems = filtered.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / normalizedSize);
        long offset = (long) normalizedPage * normalizedSize;
        if (offset >= totalItems) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, totalItems, totalPages);
        }
        int fromIndex = (int) offset;
        int toIndex = (int) Math.min(offset + normalizedSize, totalItems);
        return new PageResult<>(filtered.subList(fromIndex, toIndex), normalizedPage, normalizedSize,
                totalItems, totalPages);
    }

    public ActivityRecord activity(String userId, String activityId) {
        requireUserId(userId);
        if (!StringUtils.hasText(activityId)) {
            throw new IllegalArgumentException("activityId is required");
        }
        return datamartRepository.findActivity(userId, activityId)
                .orElseThrow(() -> new NoSuchElementException("Activity not found: " + activityId));
    }

    public Map<String, DataCoverage> dataCoverage(String userId) {
        requireUserId(userId);
        ZoneId zone = zoneForSubject(userId);
        Map<String, DataCoverage> coverage = new LinkedHashMap<>();
        for (String metric : metrics(userId)) {
            List<TimeSeriesPoint> points = datamartRepository.readPoints(userId, metric);
            if (points.isEmpty()) {
                continue;
            }
            List<Instant> starts = points.stream()
                    .map(TimeSeriesPoint::periodStart)
                    .filter(value -> value != null)
                    .sorted()
                    .toList();
            if (starts.isEmpty()) {
                continue;
            }
            LocalDate firstDate = starts.get(0).atZone(zone).toLocalDate();
            LocalDate lastDate = starts.get(starts.size() - 1).atZone(zone).toLocalDate();
            Set<LocalDate> datesWithData = points.stream()
                    .map(TimeSeriesPoint::periodStart)
                    .filter(value -> value != null)
                    .map(value -> value.atZone(zone).toLocalDate())
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
            String period = points.get(0).period();
            long expectedDays = "P1D".equals(period)
                    ? ChronoUnit.DAYS.between(firstDate, lastDate) + 1
                    : datesWithData.size();
            coverage.put(metric, new DataCoverage(
                    starts.get(0),
                    starts.get(starts.size() - 1),
                    datesWithData.size(),
                    Math.max(0, expectedDays - datesWithData.size()),
                    points.size(),
                    period,
                    points.get(0).unit()
            ));
        }
        return coverage;
    }

    public List<ProviderSyncStatus> syncStatus(String userId) {
        return providers(userId);
    }

    public List<SyncHistoryEntry> syncHistory(String userId) {
        requireUserId(userId);
        return datamartRepository.readSyncHistory(userId);
    }

    public Map<String, List<TimeSeriesPoint>> allTimeSeries(String userId) {
        requireUserId(userId);
        Map<String, List<TimeSeriesPoint>> series = new LinkedHashMap<>();
        metrics(userId).forEach(metric -> series.put(metric, timeSeries(userId, metric, null, null)));
        return series;
    }

    public DatasetExport exportDataset(String userId) {
        requireUserId(userId);
        return datamartRepository.describeExport(userId);
    }

    private ZoneId zoneForSubject(String userId) {
        return datamartRepository.findSubject(userId)
                .map(SubjectInfo::timezone)
                .filter(StringUtils::hasText)
                .map(this::safeZone)
                .orElse(ZoneOffset.UTC);
    }

    private ZoneId safeZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ignored) {
            return ZoneOffset.UTC;
        }
    }

    private boolean withinRange(TimeSeriesPoint point, ZoneId zone, LocalDate from, LocalDate to) {
        LocalDate pointDate = point.periodStart().atZone(zone).toLocalDate();
        if (from != null && pointDate.isBefore(from)) {
            return false;
        }
        return to == null || !pointDate.isAfter(to);
    }

    private boolean activityWithinRange(ActivityRecord activity, ZoneId zone, LocalDate from, LocalDate to) {
        if (activity.startTime() == null) {
            return from == null && to == null;
        }
        LocalDate activityDate = activity.startTime().atZone(zone).toLocalDate();
        if (from != null && activityDate.isBefore(from)) {
            return false;
        }
        return to == null || !activityDate.isAfter(to);
    }

    private void requireUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
    }

    private BigDecimal sumOrNull(List<TimeSeriesPoint> points) {
        if (points.isEmpty()) {
            return null;
        }
        return points.stream()
                .map(TimeSeriesPoint::value)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .stripTrailingZeros();
    }

    private BigDecimal lastOrNull(List<TimeSeriesPoint> points) {
        return points.stream()
                .sorted(pointComparator().reversed())
                .map(TimeSeriesPoint::value)
                .filter(value -> value != null)
                .findFirst()
                .map(BigDecimal::stripTrailingZeros)
                .orElse(null);
    }

    private BigDecimal sum(List<TimeSeriesPoint> points, String metric) {
        return points.stream()
                .filter(point -> metric.equals(point.metricName()))
                .map(TimeSeriesPoint::value)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .stripTrailingZeros();
    }

    private Map<String, BigDecimal> sumByActivityType(List<TimeSeriesPoint> points, String metric) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        points.stream()
                .filter(point -> metric.equals(point.metricName()))
                .filter(point -> StringUtils.hasText(point.activityType()))
                .filter(point -> point.value() != null)
                .sorted(Comparator.comparing(point -> point.activityType().toLowerCase(Locale.ROOT)))
                .forEach(point -> values.merge(point.activityType(), point.value(), BigDecimal::add));
        Map<String, BigDecimal> stripped = new LinkedHashMap<>();
        values.forEach((activityType, value) -> stripped.put(activityType, value.stripTrailingZeros()));
        return stripped;
    }

    private Comparator<TimeSeriesPoint> pointComparator() {
        return Comparator
                .comparing(TimeSeriesPoint::periodStart)
                .thenComparing(TimeSeriesPoint::metricName)
                .thenComparing(point -> point.activityType() == null ? "" : point.activityType())
                .thenComparing(point -> point.zone() == null ? "" : point.zone());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record DailySummary(
            LocalDate date,
            BigDecimal steps,
            BigDecimal distanceMeters,
            BigDecimal sleepDuration,
            BigDecimal calories,
            BigDecimal weight,
            BigDecimal activities
    ) {
    }

    public record WeeklyActivitySummary(
            LocalDate from,
            LocalDate to,
            BigDecimal workoutCount,
            BigDecimal workoutDuration,
            BigDecimal distance,
            BigDecimal calories,
            Map<String, BigDecimal> durationBySport,
            Map<String, BigDecimal> distanceBySport,
            Map<String, BigDecimal> caloriesBySport
    ) {
    }

    public record DataCoverage(
            Instant firstRecord,
            Instant lastRecord,
            long daysWithData,
            long missingDays,
            long pointCount,
            String period,
            String unit
    ) {
    }
}
