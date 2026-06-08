package com.h2traindata.dataapp.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ApiV1Dtos {

    private ApiV1Dtos() {
    }

    public record UserResponse(
            String userId,
            List<String> providers,
            String timezone,
            Instant firstRecord,
            Instant lastRecord
    ) {
    }

    public record ProviderResponse(
            String provider,
            Boolean syncEnabled,
            String syncInterval,
            Instant lastSync,
            String status
    ) {
    }

    public record TimeSeriesPointResponse(
            Instant periodStart,
            Instant periodEnd,
            BigDecimal value,
            String unit,
            String period,
            String aggregationType,
            String provider,
            String activityType,
            String zone
    ) {
    }

    public record TimeSeriesResponse(
            String userId,
            String metric,
            String unit,
            String resolution,
            LocalDate from,
            LocalDate to,
            List<TimeSeriesPointResponse> points
    ) {
    }

    public record MetricSeriesResponse(
            String metric,
            String unit,
            List<TimeSeriesPointResponse> points
    ) {
    }

    public record TimeSeriesBatchResponse(
            String userId,
            LocalDate from,
            LocalDate to,
            List<MetricSeriesResponse> series
    ) {
    }

    public record DailySummaryResponse(
            String userId,
            LocalDate date,
            BigDecimal steps,
            BigDecimal distanceMeters,
            BigDecimal sleepDuration,
            BigDecimal calories,
            BigDecimal weight,
            BigDecimal activities
    ) {
    }

    public record WeeklySummaryResponse(
            String userId,
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

    public record ActivityResponse(
            String activityId,
            String userId,
            String provider,
            String activityType,
            Instant startTime,
            Instant endTime,
            BigDecimal duration,
            BigDecimal distanceMeters,
            BigDecimal calories,
            Instant updatedAt
    ) {
    }

    public record PageResponse<T>(
            List<T> items,
            int page,
            int size,
            long totalItems,
            int totalPages
    ) {
    }

    public record DataCoverageResponse(
            Instant firstRecord,
            Instant lastRecord,
            long daysWithData,
            long missingDays,
            long pointCount,
            String period,
            String unit
    ) {
    }

    public record SyncHistoryResponse(
            String syncId,
            String userId,
            String provider,
            Boolean syncEnabled,
            String syncInterval,
            Instant syncedAt,
            String status
    ) {
    }

    public record DatasetExportResponse(
            String userId,
            UserResponse subject,
            int metricCount,
            int pointCount,
            List<String> metrics,
            Map<String, List<TimeSeriesPointResponse>> timeseries
    ) {
    }

    public record DatasetMetadataResponse(
            int subjectCount,
            int metricCount,
            int pointCount,
            Instant firstRecord,
            Instant lastRecord,
            List<String> metrics
    ) {
    }

    public record HealthResponse(
            String status,
            String service
    ) {
    }
}
