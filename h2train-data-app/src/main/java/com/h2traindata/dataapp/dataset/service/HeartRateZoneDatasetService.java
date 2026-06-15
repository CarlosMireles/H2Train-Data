package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetRequest;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetResponse;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDay;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneValue;
import com.h2traindata.dataapp.dataset.exception.InvalidDatasetQueryException;
import com.h2traindata.dataapp.dataset.exception.UnsupportedMetricException;
import com.h2traindata.dataapp.dataset.format.DatasetFormat;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HeartRateZoneDatasetService {

    private static final String MINUTES_METRIC = "heart_rate_zone_minutes";
    private static final String CALORIES_METRIC = "heart_rate_zone_calories";
    private static final Set<String> HIGH_INTENSITY_ZONES = Set.of("cardio", "peak");
    private static final String INACTIVE_ZONE = "out_of_range";

    private final LongitudinalDatasetReader reader;

    public HeartRateZoneDatasetService(LongitudinalDatasetReader reader) {
        this.reader = reader;
    }

    public HeartRateZoneDatasetResult query(HeartRateZoneDatasetRequest request) {
        ValidatedRequest validated = validate(request);
        List<HeartRateZoneDay> days = new ArrayList<>();
        reader.subjects().stream()
                .sorted(Comparator.comparing(SubjectInfo::userId))
                .forEach(subject -> days.addAll(daysForSubject(subject, validated)));
        return new HeartRateZoneDatasetResult(
                validated.format(),
                new HeartRateZoneDatasetResponse(validated.normalizedRequest(), List.copyOf(days))
        );
    }

    private List<HeartRateZoneDay> daysForSubject(SubjectInfo subject, ValidatedRequest request) {
        ZoneId timezone = safeZone(subject.timezone());
        Map<DayProviderKey, Map<String, MutableZoneValue>> values = new LinkedHashMap<>();
        addPoints(values, reader.readPoints(subject.userId(), MINUTES_METRIC, request.from(), request.to()),
                timezone, request.providers(), true);
        addPoints(values, reader.readPoints(subject.userId(), CALORIES_METRIC, request.from(), request.to()),
                timezone, request.providers(), false);
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toDay(subject.userId(), entry.getKey(), entry.getValue(), request.zones()))
                .filter(day -> !day.zones().isEmpty())
                .toList();
    }

    private void addPoints(Map<DayProviderKey, Map<String, MutableZoneValue>> values,
                           List<TimeSeriesPoint> points,
                           ZoneId timezone,
                           Set<String> providers,
                           boolean minutes) {
        for (TimeSeriesPoint point : points) {
            if (point.periodStart() == null || point.value() == null) {
                continue;
            }
            String provider = normalizedOrUnknown(point.provider());
            if (!providers.isEmpty() && !providers.contains(provider)) {
                continue;
            }
            DayProviderKey key = new DayProviderKey(
                    point.periodStart().atZone(timezone).toLocalDate(),
                    provider
            );
            String zone = normalizedOrUnknown(point.zone());
            MutableZoneValue value = values
                    .computeIfAbsent(key, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(zone, ignored -> new MutableZoneValue());
            if (minutes) {
                value.minutes = value.minutes.add(point.value());
            } else {
                value.calories = value.calories.add(point.value());
            }
        }
    }

    private HeartRateZoneDay toDay(String userId,
                                   DayProviderKey key,
                                   Map<String, MutableZoneValue> values,
                                   Set<String> requestedZones) {
        BigDecimal trackedMinutes = sum(values, true, false);
        BigDecimal activeMinutes = sum(values, true, true);
        BigDecimal totalCalories = sum(values, false, false);
        BigDecimal activeCalories = sum(values, false, true);
        BigDecimal highIntensityMinutes = values.entrySet().stream()
                .filter(entry -> HIGH_INTENSITY_ZONES.contains(entry.getKey()))
                .map(entry -> entry.getValue().minutes)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String dominantActiveZone = values.entrySet().stream()
                .filter(entry -> !INACTIVE_ZONE.equals(entry.getKey()))
                .filter(entry -> entry.getValue().minutes.compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator
                        .comparing((Map.Entry<String, MutableZoneValue> entry) -> entry.getValue().minutes)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
        List<HeartRateZoneValue> zones = values.entrySet().stream()
                .filter(entry -> requestedZones.isEmpty() || requestedZones.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new HeartRateZoneValue(
                        entry.getKey(),
                        clean(entry.getValue().minutes),
                        clean(entry.getValue().calories),
                        percentage(entry.getValue().minutes, trackedMinutes),
                        INACTIVE_ZONE.equals(entry.getKey())
                                ? BigDecimal.ZERO
                                : percentage(entry.getValue().minutes, activeMinutes)
                ))
                .toList();
        return new HeartRateZoneDay(
                userId,
                key.date(),
                key.provider(),
                clean(trackedMinutes),
                clean(activeMinutes),
                clean(totalCalories),
                clean(activeCalories),
                clean(highIntensityMinutes),
                dominantActiveZone,
                zones
        );
    }

    private BigDecimal sum(Map<String, MutableZoneValue> values, boolean minutes, boolean activeOnly) {
        return values.entrySet().stream()
                .filter(entry -> !activeOnly || !INACTIVE_ZONE.equals(entry.getKey()))
                .map(entry -> minutes ? entry.getValue().minutes : entry.getValue().calories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }

    private BigDecimal clean(BigDecimal value) {
        return value.stripTrailingZeros();
    }

    private ValidatedRequest validate(HeartRateZoneDatasetRequest request) {
        if (request == null) {
            throw new InvalidDatasetQueryException("query is required");
        }
        if (request.from() != null && request.to() != null && request.from().isAfter(request.to())) {
            throw new InvalidDatasetQueryException("from must be before or equal to to");
        }
        if (!reader.availableMetrics().contains(MINUTES_METRIC)) {
            throw new UnsupportedMetricException(MINUTES_METRIC);
        }
        DatasetFormat format = DatasetFormat.parse(required(request.format(), "format"));
        Set<String> zones = normalizeValues(request.zones());
        Set<String> providers = normalizeValues(request.providers());
        HeartRateZoneDatasetRequest normalized = new HeartRateZoneDatasetRequest(
                request.from(),
                request.to(),
                List.copyOf(zones),
                List.copyOf(providers),
                format.value()
        );
        return new ValidatedRequest(request.from(), request.to(), zones, providers, format, normalized);
    }

    private Set<String> normalizeValues(List<String> values) {
        if (values == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        values.stream()
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .forEach(normalized::add);
        return Collections.unmodifiableSet(normalized);
    }

    private String required(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidDatasetQueryException(field + " is required");
        }
        return value;
    }

    private String normalizedOrUnknown(String value) {
        return StringUtils.hasText(value) ? normalize(value) : "unknown";
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private ZoneId safeZone(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ignored) {
            return ZoneOffset.UTC;
        }
    }

    private record ValidatedRequest(
            LocalDate from,
            LocalDate to,
            Set<String> zones,
            Set<String> providers,
            DatasetFormat format,
            HeartRateZoneDatasetRequest normalizedRequest
    ) {
    }

    private record DayProviderKey(LocalDate date, String provider) implements Comparable<DayProviderKey> {
        @Override
        public int compareTo(DayProviderKey other) {
            int dateComparison = date.compareTo(other.date);
            return dateComparison != 0 ? dateComparison : provider.compareTo(other.provider);
        }
    }

    private static final class MutableZoneValue {
        private BigDecimal minutes = BigDecimal.ZERO;
        private BigDecimal calories = BigDecimal.ZERO;
    }
}
