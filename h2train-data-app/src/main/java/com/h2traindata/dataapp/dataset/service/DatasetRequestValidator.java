package com.h2traindata.dataapp.dataset.service;

import com.h2traindata.dataapp.dataset.aggregation.DatasetAggregation;
import com.h2traindata.dataapp.dataset.dto.DatasetExportRequest;
import com.h2traindata.dataapp.dataset.dto.DatasetFilterRequest;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryRequest;
import com.h2traindata.dataapp.dataset.exception.InvalidDatasetQueryException;
import com.h2traindata.dataapp.dataset.exception.UnsupportedMetricException;
import com.h2traindata.dataapp.dataset.filter.DatasetDimension;
import com.h2traindata.dataapp.dataset.filter.DatasetDimensions;
import com.h2traindata.dataapp.dataset.filter.DatasetFilter;
import com.h2traindata.dataapp.dataset.filter.DatasetOperator;
import com.h2traindata.dataapp.dataset.format.DatasetFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatasetRequestValidator {

    private final LongitudinalDatasetReader reader;

    public DatasetRequestValidator(LongitudinalDatasetReader reader) {
        this.reader = reader;
    }

    public ValidatedQuery validate(DatasetQueryRequest request) {
        if (request == null) {
            throw new InvalidDatasetQueryException("query is required");
        }
        Set<String> availableMetrics = reader.availableMetrics();
        String metric = validateMetric(request.metric(), availableMetrics);
        DatasetFilter filter = validateFilter(new DatasetFilterRequest(
                metric,
                request.operator(),
                request.value(),
                request.maxValue(),
                request.aggregation(),
                request.dimensions()
        ), availableMetrics);
        validateRange(request.from(), request.to());
        DatasetFormat format = DatasetFormat.parse(required(request.format(), "format"));
        DatasetQueryRequest normalized = new DatasetQueryRequest(
                metric,
                filter.operator().value(),
                filter.value(),
                filter.maxValue(),
                filter.aggregation().value(),
                filter.dimensions().externalValues(),
                request.from(),
                request.to(),
                format.value()
        );
        return new ValidatedQuery(filter, request.from(), request.to(), format, normalized);
    }

    public ValidatedExport validate(DatasetExportRequest request) {
        if (request == null) {
            throw new InvalidDatasetQueryException("request body is required");
        }
        Set<String> availableMetrics = reader.availableMetrics();
        List<String> metrics = normalizeMetrics(request.metrics(), availableMetrics);
        List<DatasetFilter> filters = request.filters() == null
                ? List.of()
                : request.filters().stream()
                        .map(filter -> validateFilter(filter, availableMetrics))
                        .toList();
        DatasetDimensions dimensions = validateDimensions(request.dimensions());
        validateRange(request.from(), request.to());
        DatasetFormat format = DatasetFormat.parse(required(request.format(), "format"));
        List<DatasetFilterRequest> normalizedFilters = filters.stream()
                .map(this::toRequest)
                .toList();
        DatasetExportRequest normalized = new DatasetExportRequest(
                metrics,
                normalizedFilters,
                dimensions.externalValues(),
                request.from(),
                request.to(),
                format.value()
        );
        return new ValidatedExport(
                metrics,
                filters,
                dimensions,
                request.from(),
                request.to(),
                format,
                normalized
        );
    }

    private DatasetFilter validateFilter(DatasetFilterRequest request, Set<String> availableMetrics) {
        if (request == null) {
            throw new InvalidDatasetQueryException("filter is required");
        }
        String metric = validateMetric(request.metric(), availableMetrics);
        DatasetOperator operator = DatasetOperator.parse(required(request.operator(), "operator"));
        DatasetAggregation aggregation =
                DatasetAggregation.parse(required(request.aggregation(), "aggregation"));
        BigDecimal value = request.value();
        if (value == null) {
            throw new InvalidDatasetQueryException("value is required");
        }
        BigDecimal maxValue = request.maxValue();
        if (operator == DatasetOperator.BETWEEN) {
            if (maxValue == null) {
                throw new InvalidDatasetQueryException("maxValue is required for between");
            }
            if (value.compareTo(maxValue) > 0) {
                throw new InvalidDatasetQueryException("value must be less than or equal to maxValue");
            }
        }
        return new DatasetFilter(
                metric,
                operator,
                value,
                maxValue,
                aggregation,
                validateDimensions(request.dimensions())
        );
    }

    private String validateMetric(String value, Set<String> availableMetrics) {
        String metric = normalize(required(value, "metric"));
        if (!availableMetrics.contains(metric)) {
            throw new UnsupportedMetricException(metric);
        }
        return metric;
    }

    private List<String> normalizeMetrics(List<String> requestedMetrics, Set<String> availableMetrics) {
        if (requestedMetrics == null || requestedMetrics.stream().noneMatch(StringUtils::hasText)) {
            throw new InvalidDatasetQueryException("metrics is required");
        }
        Set<String> metrics = new LinkedHashSet<>();
        requestedMetrics.stream()
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .forEach(metric -> {
                    if (!availableMetrics.contains(metric)) {
                        throw new UnsupportedMetricException(metric);
                    }
                    metrics.add(metric);
                });
        return List.copyOf(metrics);
    }

    private DatasetFilterRequest toRequest(DatasetFilter filter) {
        return new DatasetFilterRequest(
                filter.metric(),
                filter.operator().value(),
                filter.value(),
                filter.maxValue(),
                filter.aggregation().value(),
                filter.dimensions().externalValues()
        );
    }

    private DatasetDimensions validateDimensions(Map<String, List<String>> requestedDimensions) {
        if (requestedDimensions == null || requestedDimensions.isEmpty()) {
            return DatasetDimensions.empty();
        }
        Map<DatasetDimension, Set<String>> dimensions = new EnumMap<>(DatasetDimension.class);
        requestedDimensions.forEach((name, requestedValues) -> {
            DatasetDimension dimension = DatasetDimension.parse(name);
            if (requestedValues == null || requestedValues.stream().noneMatch(StringUtils::hasText)) {
                throw new InvalidDatasetQueryException(
                        "At least one value is required for dimension " + dimension.value()
                );
            }
            Set<String> values = requestedValues.stream()
                    .filter(StringUtils::hasText)
                    .map(this::normalize)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            dimensions.merge(dimension, values, (existing, additional) -> {
                Set<String> merged = new LinkedHashSet<>(existing);
                merged.addAll(additional);
                return merged;
            });
        });
        return new DatasetDimensions(dimensions);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDatasetQueryException("from must be before or equal to to");
        }
    }

    private String required(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidDatasetQueryException(field + " is required");
        }
        return value;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record ValidatedQuery(
            DatasetFilter filter,
            LocalDate from,
            LocalDate to,
            DatasetFormat format,
            DatasetQueryRequest normalizedRequest
    ) {
    }

    public record ValidatedExport(
            List<String> metrics,
            List<DatasetFilter> filters,
            DatasetDimensions dimensions,
            LocalDate from,
            LocalDate to,
            DatasetFormat format,
            DatasetExportRequest normalizedRequest
    ) {
    }
}
