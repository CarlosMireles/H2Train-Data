package com.h2traindata.dataapp.dataset.filter;

import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DatasetDimensions {

    private static final DatasetDimensions EMPTY = new DatasetDimensions(Map.of());

    private final Map<DatasetDimension, Set<String>> values;

    public DatasetDimensions(Map<DatasetDimension, Set<String>> values) {
        Map<DatasetDimension, Set<String>> copy = new EnumMap<>(DatasetDimension.class);
        values.forEach((dimension, dimensionValues) -> copy.put(
                dimension,
                dimensionValues.stream()
                        .map(DatasetDimensions::normalize)
                        .collect(Collectors.toUnmodifiableSet())
        ));
        this.values = Map.copyOf(copy);
    }

    public static DatasetDimensions empty() {
        return EMPTY;
    }

    public boolean matches(TimeSeriesPoint point) {
        return values.entrySet().stream().allMatch(entry -> {
            String pointValue = entry.getKey().valueFrom(point);
            return pointValue != null && entry.getValue().contains(normalize(pointValue));
        });
    }

    public List<TimeSeriesPoint> filter(List<TimeSeriesPoint> points) {
        return values.isEmpty() ? points : points.stream().filter(this::matches).toList();
    }

    public Map<String, List<String>> externalValues() {
        Map<String, List<String>> external = new LinkedHashMap<>();
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> external.put(
                        entry.getKey().value(),
                        entry.getValue().stream().sorted().toList()
                ));
        return Map.copyOf(external);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
