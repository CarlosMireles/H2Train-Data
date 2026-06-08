package com.h2traindata.dataapp.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.h2traindata.dataapp.domain.AggregationType;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesContribution;
import com.h2traindata.dataapp.domain.TimeSeriesDataset;
import com.h2traindata.dataapp.domain.TimeSeriesMetric;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TimeSeriesBuilderService {

    public static final String DAILY_PERIOD = "P1D";
    public static final String WEEKLY_PERIOD = "P1W";

    private static final ZoneId DEFAULT_ZONE = ZoneOffset.UTC;

    private final Clock clock;

    public TimeSeriesBuilderService() {
        this(Clock.systemUTC());
    }

    TimeSeriesBuilderService(Clock clock) {
        this.clock = clock;
    }

    public List<TimeSeriesContribution> projectEvent(NormalizedDatalakeEvent event, ZoneId zone) {
        if (event == null || event.eventTimestamp() == null || event.event() == null) {
            return List.of();
        }
        ZoneId effectiveZone = zone != null ? zone : DEFAULT_ZONE;
        Instant generatedAt = Instant.now(clock);
        Map<AggregateKey, MutableAggregate> sums = new LinkedHashMap<>();
        Map<AggregateKey, LastAggregate> lasts = new LinkedHashMap<>();
        routeEvent(event, effectiveZone, sums, lasts, new HashSet<>());

        List<TimeSeriesContribution> contributions = new ArrayList<>();
        for (MutableAggregate aggregate : sums.values()) {
            TimeSeriesPoint point = aggregate.toPoint(generatedAt);
            contributions.add(new TimeSeriesContribution(
                    point,
                    isPrimaryDailyMetric(point),
                    isFallbackDailyMetric(point),
                    true,
                    event.eventTimestamp()
            ));
        }
        for (LastAggregate aggregate : lasts.values()) {
            TimeSeriesPoint point = aggregate.toPoint(generatedAt);
            contributions.add(new TimeSeriesContribution(
                    point,
                    false,
                    false,
                    true,
                    event.eventTimestamp()
            ));
        }
        return List.copyOf(contributions);
    }

    public TimeSeriesDataset buildDataset(Collection<NormalizedDatalakeEvent> sourceEvents) {
        List<NormalizedDatalakeEvent> events = deduplicate(sourceEvents);
        List<SubjectInfo> subjects = buildSubjects(events);
        Map<String, ZoneId> zonesByUser = zonesByUser(subjects);
        Instant generatedAt = Instant.now(clock);

        Map<AggregateKey, MutableAggregate> sums = new LinkedHashMap<>();
        Map<AggregateKey, LastAggregate> lasts = new LinkedHashMap<>();
        Set<DailyMetricKey> primaryDailyVitals = new HashSet<>();

        for (NormalizedDatalakeEvent event : events) {
            ZoneId zone = zonesByUser.getOrDefault(event.userId(), DEFAULT_ZONE);
            routeEvent(event, zone, sums, lasts, primaryDailyVitals);
        }

        List<TimeSeriesPoint> points = new ArrayList<>();
        for (MutableAggregate aggregate : sums.values()) {
            if (aggregate.activitySummaryFallback()
                    && primaryDailyVitals.contains(DailyMetricKey.from(aggregate.key()))) {
                continue;
            }
            points.add(aggregate.toPoint(generatedAt));
        }
        for (LastAggregate aggregate : lasts.values()) {
            points.add(aggregate.toPoint(generatedAt));
        }
        points.sort(pointComparator());

        return new TimeSeriesDataset(subjects, List.copyOf(points));
    }

    private void routeEvent(NormalizedDatalakeEvent event,
                            ZoneId zone,
                            Map<AggregateKey, MutableAggregate> sums,
                            Map<AggregateKey, LastAggregate> lasts,
                            Set<DailyMetricKey> primaryDailyVitals) {
        if (isEvent(event, "PHYSIOLOGICAL", "Steps")) {
            addDailySum(event, zone, sums, primaryDailyVitals, TimeSeriesMetric.DAILY_STEPS, "steps", "steps", false);
            return;
        }
        if (isEvent(event, "PHYSIOLOGICAL", "Distance")) {
            addDailySum(event, zone, sums, primaryDailyVitals, TimeSeriesMetric.DAILY_DISTANCE, "distanceMeters", "m", false);
            return;
        }
        if (isEvent(event, "PHYSIOLOGICAL", "CaloriesBurned")) {
            addDailySum(event, zone, sums, primaryDailyVitals, TimeSeriesMetric.DAILY_CALORIES, "calories", "kcal", false);
            return;
        }
        if (isEvent(event, "ACTIVITY", "ActivitySummary")) {
            addDailySum(event, zone, sums, primaryDailyVitals, TimeSeriesMetric.DAILY_STEPS, "steps", "steps", true);
            addDailySum(event, zone, sums, primaryDailyVitals, TimeSeriesMetric.DAILY_DISTANCE, "distanceMeters", "m", true);
            addDailySum(event, zone, sums, primaryDailyVitals, TimeSeriesMetric.DAILY_CALORIES, "caloriesOut", "kcal", true);
            return;
        }
        if (isEvent(event, "HEALTH", "Sleep")) {
            addSleep(event, zone, sums);
            return;
        }
        if (isEvent(event, "BODY_COMPOSITION", "BodyComposition")) {
            addBodyComposition(event, zone, lasts);
            return;
        }
        if (isEvent(event, "ACTIVITY", "Workout")) {
            addWorkout(event, zone, sums);
            return;
        }
        if (isEvent(event, "PHYSIOLOGICAL", "HeartRate")) {
            addHeartRateZone(event, zone, sums);
        }
    }

    private void addDailySum(NormalizedDatalakeEvent event,
                             ZoneId zone,
                             Map<AggregateKey, MutableAggregate> sums,
                             Set<DailyMetricKey> primaryDailyVitals,
                             TimeSeriesMetric metric,
                             String fieldName,
                             String unit,
                             boolean activitySummaryFallback) {
        BigDecimal value = numberField(event.event(), fieldName).orElse(null);
        if (value == null) {
            return;
        }
        PeriodRange period = dailyPeriod(event.eventTimestamp(), zone);
        AggregateKey key = key(event, metric.metricName(), DAILY_PERIOD, period, unit, AggregationType.SUM, null, null);
        addSum(sums, key, value, activitySummaryFallback);
        if (!activitySummaryFallback) {
            primaryDailyVitals.add(DailyMetricKey.from(key));
        }
    }

    private void addSleep(NormalizedDatalakeEvent event, ZoneId zone, Map<AggregateKey, MutableAggregate> sums) {
        BigDecimal duration = numberField(event.event(), "duration").orElse(null);
        if (duration == null) {
            return;
        }
        Instant startTime = instantField(event.event(), "startTime").orElse(event.eventTimestamp());
        PeriodRange period = dailyPeriod(startTime, zone);
        AggregateKey key = key(
                event,
                TimeSeriesMetric.DAILY_SLEEP_DURATION.metricName(),
                DAILY_PERIOD,
                period,
                "s",
                AggregationType.SUM,
                null,
                null
        );
        addSum(sums, key, duration, false);
    }

    private void addBodyComposition(NormalizedDatalakeEvent event,
                                    ZoneId zone,
                                    Map<AggregateKey, LastAggregate> lasts) {
        addLastMetric(event, zone, lasts, TimeSeriesMetric.DAILY_WEIGHT, "weight", "kg");
        addLastMetric(event, zone, lasts, TimeSeriesMetric.DAILY_BMI, "bodyMassIndex", "kg/m2");
        addLastMetric(event, zone, lasts, TimeSeriesMetric.DAILY_BODY_FAT_PERCENTAGE, "bodyFatPercentage", "%");
    }

    private void addLastMetric(NormalizedDatalakeEvent event,
                               ZoneId zone,
                               Map<AggregateKey, LastAggregate> lasts,
                               TimeSeriesMetric metric,
                               String fieldName,
                               String unit) {
        BigDecimal value = numberField(event.event(), fieldName).orElse(null);
        if (value == null) {
            return;
        }
        PeriodRange period = dailyPeriod(event.eventTimestamp(), zone);
        AggregateKey key = key(event, metric.metricName(), DAILY_PERIOD, period, unit, AggregationType.LAST, null, null);
        lasts.compute(key, (ignored, existing) -> {
            if (existing == null || isSameOrAfter(event.eventTimestamp(), existing.observedAt())) {
                return new LastAggregate(key, value, event.eventTimestamp());
            }
            return existing;
        });
    }

    private void addWorkout(NormalizedDatalakeEvent event, ZoneId zone, Map<AggregateKey, MutableAggregate> sums) {
        Instant startTime = instantField(event.event(), "startTime").orElse(event.eventTimestamp());
        String activityType = normalizedDimension(textField(event.event(), "activityType").orElse("workout"));
        PeriodRange day = dailyPeriod(startTime, zone);
        PeriodRange week = weeklyPeriod(startTime, zone);

        addCount(sums, key(event, TimeSeriesMetric.DAILY_ACTIVITY_COUNT.metricName(), DAILY_PERIOD, day, "count",
                AggregationType.COUNT, null, null));
        addNumericWorkoutMetric(event, sums, TimeSeriesMetric.DAILY_WORKOUT_DURATION, DAILY_PERIOD, day,
                "duration", "s", null);
        addNumericWorkoutMetric(event, sums, TimeSeriesMetric.DAILY_WORKOUT_DISTANCE, DAILY_PERIOD, day,
                "distanceMeters", "m", null);
        addNumericWorkoutMetric(event, sums, TimeSeriesMetric.DAILY_WORKOUT_CALORIES, DAILY_PERIOD, day,
                "calories", "kcal", null);

        addCount(sums, key(event, TimeSeriesMetric.WEEKLY_ACTIVITY_COUNT.metricName(), WEEKLY_PERIOD, week, "count",
                AggregationType.COUNT, null, null));
        addNumericWorkoutMetric(event, sums, TimeSeriesMetric.WEEKLY_WORKOUT_DURATION, WEEKLY_PERIOD, week,
                "duration", "s", activityType);
        addNumericWorkoutMetric(event, sums, TimeSeriesMetric.WEEKLY_WORKOUT_DISTANCE_BY_SPORT, WEEKLY_PERIOD, week,
                "distanceMeters", "m", activityType);
        addNumericWorkoutMetric(event, sums, TimeSeriesMetric.WEEKLY_WORKOUT_CALORIES_BY_SPORT, WEEKLY_PERIOD, week,
                "calories", "kcal", activityType);
    }

    private void addNumericWorkoutMetric(NormalizedDatalakeEvent event,
                                         Map<AggregateKey, MutableAggregate> sums,
                                         TimeSeriesMetric metric,
                                         String periodName,
                                         PeriodRange period,
                                         String fieldName,
                                         String unit,
                                         String activityType) {
        BigDecimal value = numberField(event.event(), fieldName).orElse(null);
        if (value == null) {
            return;
        }
        AggregateKey key = key(event, metric.metricName(), periodName, period, unit, AggregationType.SUM, activityType, null);
        addSum(sums, key, value, false);
    }

    private void addHeartRateZone(NormalizedDatalakeEvent event,
                                  ZoneId zone,
                                  Map<AggregateKey, MutableAggregate> sums) {
        String zoneName = textField(event.event(), "zone")
                .map(TimeSeriesBuilderService::normalizedDimension)
                .orElse(null);
        PeriodRange period = dailyPeriod(event.eventTimestamp(), zone);
        numberField(event.event(), "minutes").ifPresent(minutes -> addSum(
                sums,
                key(event, TimeSeriesMetric.HEART_RATE_ZONE_MINUTES.metricName(), DAILY_PERIOD, period, "min",
                        AggregationType.SUM, null, zoneName),
                minutes,
                false
        ));
        numberField(event.event(), "calories").ifPresent(calories -> addSum(
                sums,
                key(event, TimeSeriesMetric.HEART_RATE_ZONE_CALORIES.metricName(), DAILY_PERIOD, period, "kcal",
                        AggregationType.SUM, null, zoneName),
                calories,
                false
        ));
    }

    private void addSum(Map<AggregateKey, MutableAggregate> sums,
                        AggregateKey key,
                        BigDecimal value,
                        boolean activitySummaryFallback) {
        sums.computeIfAbsent(key, ignored -> new MutableAggregate(key, activitySummaryFallback))
                .add(value, activitySummaryFallback);
    }

    private void addCount(Map<AggregateKey, MutableAggregate> sums, AggregateKey key) {
        addSum(sums, key, BigDecimal.ONE, false);
    }

    private AggregateKey key(NormalizedDatalakeEvent event,
                             String metricName,
                             String periodName,
                             PeriodRange period,
                             String unit,
                             AggregationType aggregationType,
                             String activityType,
                             String zone) {
        return new AggregateKey(
                event.userId(),
                metricName,
                periodName,
                period.start(),
                period.end(),
                unit,
                event.providerId(),
                event.eventType(),
                event.eventName(),
                aggregationType,
                activityType,
                zone
        );
    }

    private List<NormalizedDatalakeEvent> deduplicate(Collection<NormalizedDatalakeEvent> sourceEvents) {
        Map<String, NormalizedDatalakeEvent> unique = new LinkedHashMap<>();
        int missingIdCounter = 0;
        for (NormalizedDatalakeEvent event : sourceEvents == null ? List.<NormalizedDatalakeEvent>of() : sourceEvents) {
            if (!StringUtils.hasText(event.userId()) || event.eventTimestamp() == null || event.event() == null) {
                continue;
            }
            String key = StringUtils.hasText(event.eventId())
                    ? String.join("|",
                    event.userId(),
                    nullToEmpty(event.providerId()),
                    nullToEmpty(event.eventType()),
                    nullToEmpty(event.eventName()),
                    event.eventId())
                    : "missing-event-id-" + missingIdCounter++;
            unique.compute(key, (ignored, existing) -> {
                if (existing == null || isSameOrAfter(observedPublication(event), observedPublication(existing))) {
                    return event;
                }
                return existing;
            });
        }
        return unique.values().stream()
                .sorted(Comparator
                        .comparing(NormalizedDatalakeEvent::eventTimestamp)
                        .thenComparing(event -> nullToEmpty(event.userId()))
                        .thenComparing(event -> nullToEmpty(event.eventId())))
                .toList();
    }

    private List<SubjectInfo> buildSubjects(List<NormalizedDatalakeEvent> events) {
        Map<String, SubjectAccumulator> subjects = new LinkedHashMap<>();
        for (NormalizedDatalakeEvent event : events) {
            subjects.computeIfAbsent(event.userId(), SubjectAccumulator::new).accept(event);
        }
        return subjects.values().stream()
                .map(SubjectAccumulator::toSubjectInfo)
                .sorted(Comparator.comparing(SubjectInfo::subjectId))
                .toList();
    }

    private Map<String, ZoneId> zonesByUser(List<SubjectInfo> subjects) {
        Map<String, ZoneId> zones = new HashMap<>();
        for (SubjectInfo subject : subjects) {
            zones.put(subject.userId(), zoneId(subject.timezone()));
        }
        return zones;
    }

    private PeriodRange dailyPeriod(Instant instant, ZoneId zone) {
        LocalDate date = instant.atZone(zone).toLocalDate();
        Instant start = date.atStartOfDay(zone).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(zone).toInstant();
        return new PeriodRange(start, end);
    }

    private PeriodRange weeklyPeriod(Instant instant, ZoneId zone) {
        LocalDate date = instant.atZone(zone).toLocalDate();
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant start = monday.atStartOfDay(zone).toInstant();
        Instant end = monday.plusWeeks(1).atStartOfDay(zone).toInstant();
        return new PeriodRange(start, end);
    }

    private boolean isEvent(NormalizedDatalakeEvent event, String eventType, String eventName) {
        return eventType.equalsIgnoreCase(nullToEmpty(event.eventType()))
                && eventName.equalsIgnoreCase(nullToEmpty(event.eventName()));
    }

    private Optional<BigDecimal> numberField(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return Optional.empty();
        }
        JsonNode value = node.path(fieldName);
        if (value.isNumber()) {
            return Optional.of(value.decimalValue());
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            try {
                return Optional.of(new BigDecimal(value.asText().trim()));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<String> textField(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return Optional.empty();
        }
        String value = node.path(fieldName).asText(null);
        return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
    }

    private Optional<Instant> instantField(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(instantValue(node.path(fieldName)));
    }

    private static Instant instantValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return decimalSecondsToInstant(node.decimalValue());
        }
        if (!node.isTextual() || !StringUtils.hasText(node.asText())) {
            return null;
        }
        String value = node.asText().trim();
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    return decimalSecondsToInstant(new BigDecimal(value));
                } catch (NumberFormatException ignoredNumeric) {
                    return null;
                }
            }
        }
    }

    private static Instant decimalSecondsToInstant(BigDecimal seconds) {
        long epochSecond = seconds.longValue();
        BigDecimal fractional = seconds.subtract(BigDecimal.valueOf(epochSecond));
        int nanos = fractional.movePointRight(9).setScale(0, RoundingMode.HALF_UP).intValue();
        if (nanos >= 1_000_000_000) {
            epochSecond++;
            nanos -= 1_000_000_000;
        }
        return Instant.ofEpochSecond(epochSecond, nanos);
    }

    private static ZoneId zoneId(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ignored) {
            return DEFAULT_ZONE;
        }
    }

    private static boolean isSameOrAfter(Instant candidate, Instant reference) {
        if (candidate == null) {
            return false;
        }
        return reference == null || !candidate.isBefore(reference);
    }

    private static Instant observedPublication(NormalizedDatalakeEvent event) {
        return event.publishedAt() != null ? event.publishedAt() : event.eventTimestamp();
    }

    private static String normalizedDimension(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Comparator<TimeSeriesPoint> pointComparator() {
        return Comparator
                .comparing(TimeSeriesPoint::userId)
                .thenComparing(TimeSeriesPoint::metricName)
                .thenComparing(TimeSeriesPoint::periodStart)
                .thenComparing(point -> nullToEmpty(point.provider()))
                .thenComparing(point -> nullToEmpty(point.activityType()))
                .thenComparing(point -> nullToEmpty(point.zone()));
    }

    private boolean isPrimaryDailyMetric(TimeSeriesPoint point) {
        if (!DAILY_PERIOD.equals(point.period())) {
            return false;
        }
        return ("daily_steps".equals(point.metricName()) && "Steps".equals(point.sourceEventName()))
                || ("daily_distance".equals(point.metricName()) && "Distance".equals(point.sourceEventName()))
                || ("daily_calories".equals(point.metricName()) && "CaloriesBurned".equals(point.sourceEventName()));
    }

    private boolean isFallbackDailyMetric(TimeSeriesPoint point) {
        if (!DAILY_PERIOD.equals(point.period()) || !"ActivitySummary".equals(point.sourceEventName())) {
            return false;
        }
        return "daily_steps".equals(point.metricName())
                || "daily_distance".equals(point.metricName())
                || "daily_calories".equals(point.metricName());
    }

    private record PeriodRange(Instant start, Instant end) {
    }

    private record AggregateKey(
            String userId,
            String metricName,
            String period,
            Instant periodStart,
            Instant periodEnd,
            String unit,
            String provider,
            String sourceEventType,
            String sourceEventName,
            AggregationType aggregationType,
            String activityType,
            String zone
    ) {
    }

    private record DailyMetricKey(String userId, String provider, String metricName, Instant periodStart) {

        private static DailyMetricKey from(AggregateKey key) {
            return new DailyMetricKey(key.userId(), key.provider(), key.metricName(), key.periodStart());
        }
    }

    private static final class MutableAggregate {

        private final AggregateKey key;
        private BigDecimal value = BigDecimal.ZERO;
        private boolean activitySummaryFallback;

        private MutableAggregate(AggregateKey key, boolean activitySummaryFallback) {
            this.key = key;
            this.activitySummaryFallback = activitySummaryFallback;
        }

        private AggregateKey key() {
            return key;
        }

        private boolean activitySummaryFallback() {
            return activitySummaryFallback;
        }

        private void add(BigDecimal increment, boolean fallback) {
            value = value.add(increment);
            activitySummaryFallback = activitySummaryFallback || fallback;
        }

        private TimeSeriesPoint toPoint(Instant generatedAt) {
            return point(key, value, generatedAt);
        }
    }

    private record LastAggregate(AggregateKey key, BigDecimal value, Instant observedAt) {

        private TimeSeriesPoint toPoint(Instant generatedAt) {
            return point(key, value, generatedAt);
        }
    }

    private static TimeSeriesPoint point(AggregateKey key, BigDecimal value, Instant generatedAt) {
        return new TimeSeriesPoint(
                key.userId(),
                key.metricName(),
                key.period(),
                key.periodStart(),
                key.periodEnd(),
                value.stripTrailingZeros(),
                key.unit(),
                key.provider(),
                key.sourceEventType(),
                key.sourceEventName(),
                key.aggregationType(),
                key.activityType(),
                key.zone(),
                generatedAt
        );
    }

    private static final class SubjectAccumulator {

        private final String userId;
        private final Set<String> providers = new LinkedHashSet<>();
        private Instant firstRecord;
        private Instant lastRecord;
        private Instant profileUpdatedAt;
        private Instant bodyWeightUpdatedAt;
        private String timezone;
        private String gender;
        private BigDecimal height;
        private BigDecimal profileWeight;
        private BigDecimal bodyWeight;

        private SubjectAccumulator(String userId) {
            this.userId = userId;
        }

        private void accept(NormalizedDatalakeEvent event) {
            if (StringUtils.hasText(event.providerId())) {
                providers.add(event.providerId());
            }
            if (firstRecord == null || event.eventTimestamp().isBefore(firstRecord)) {
                firstRecord = event.eventTimestamp();
            }
            if (lastRecord == null || event.eventTimestamp().isAfter(lastRecord)) {
                lastRecord = event.eventTimestamp();
            }
            if ("USER_STATE".equalsIgnoreCase(nullToEmpty(event.eventType()))
                    && "UserProfile".equalsIgnoreCase(nullToEmpty(event.eventName()))
                    && isSameOrAfter(event.eventTimestamp(), profileUpdatedAt)) {
                profileUpdatedAt = event.eventTimestamp();
                textValue(event.event(), "timezone").ifPresent(value -> timezone = value);
                textValue(event.event(), "gender").ifPresent(value -> gender = value);
                decimalValue(event.event(), "height").ifPresent(value -> height = value);
                decimalValue(event.event(), "weight").ifPresent(value -> profileWeight = value);
            }
            if ("BODY_COMPOSITION".equalsIgnoreCase(nullToEmpty(event.eventType()))
                    && "BodyComposition".equalsIgnoreCase(nullToEmpty(event.eventName()))
                    && isSameOrAfter(event.eventTimestamp(), bodyWeightUpdatedAt)) {
                decimalValue(event.event(), "weight").ifPresent(value -> {
                    bodyWeight = value;
                    bodyWeightUpdatedAt = event.eventTimestamp();
                });
            }
        }

        private SubjectInfo toSubjectInfo() {
            return new SubjectInfo(
                    userId,
                    userId,
                    Set.copyOf(providers),
                    StringUtils.hasText(timezone) ? timezone : DEFAULT_ZONE.getId(),
                    gender,
                    height,
                    bodyWeight != null ? bodyWeight : profileWeight,
                    firstRecord,
                    lastRecord
            );
        }

        private static Optional<String> textValue(JsonNode node, String fieldName) {
            if (node == null || !node.hasNonNull(fieldName)) {
                return Optional.empty();
            }
            String value = node.path(fieldName).asText(null);
            return StringUtils.hasText(value) ? Optional.of(value.trim()) : Optional.empty();
        }

        private static Optional<BigDecimal> decimalValue(JsonNode node, String fieldName) {
            if (node == null || !node.hasNonNull(fieldName)) {
                return Optional.empty();
            }
            JsonNode value = node.path(fieldName);
            if (value.isNumber()) {
                return Optional.of(value.decimalValue());
            }
            if (value.isTextual() && StringUtils.hasText(value.asText())) {
                try {
                    return Optional.of(new BigDecimal(value.asText().trim()));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
    }
}
