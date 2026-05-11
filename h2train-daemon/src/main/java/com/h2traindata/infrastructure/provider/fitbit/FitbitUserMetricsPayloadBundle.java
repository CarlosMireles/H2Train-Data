package com.h2traindata.infrastructure.provider.fitbit;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

record FitbitUserMetricsPayloadBundle(
        Map<String, Object> profilePayload,
        Map<String, Object> lifetimeStats,
        Map<String, Object> user,
        String athleteId,
        LocalDate startDate,
        LocalDate endDate,
        FitbitHealthPayloads healthPayloads,
        FitbitSupplementalPayloads supplementalPayloads
) {
}

record FitbitHealthPayloads(
        List<Map<String, Object>> breathingRate,
        List<Map<String, Object>> vo2Max,
        List<Map<String, Object>> bloodGlucose,
        Map<String, Object> ecg,
        Map<String, Object> sleep,
        List<Map<String, Object>> spo2,
        List<Map<String, Object>> hrv,
        List<Map<String, Object>> temperatureSkin,
        List<Map<String, Object>> temperatureCore,
        Map<String, Object> irnProfile,
        Map<String, Object> irnAlerts
) {
}

record FitbitSupplementalPayloads(
        Map<String, Object> activityGoalsDaily,
        Map<String, Object> activityGoalsWeekly,
        Map<String, Object> dailyActivitySummary,
        Map<String, Object> azmTimeSeries,
        Map<String, Object> activityTimeSeriesSteps,
        Map<String, Object> activityTimeSeriesDistance,
        Map<String, Object> activityTimeSeriesCalories,
        Map<String, Object> weightLog,
        Map<String, Object> bodyFatLog,
        Map<String, Object> bodyWeightTimeSeries,
        Map<String, Object> bodyFatTimeSeries,
        Map<String, Object> bodyBmiTimeSeries,
        List<Map<String, Object>> devices,
        Map<String, Object> friends,
        Map<String, Object> friendsLeaderboard,
        Map<String, Object> heartRateTimeSeries,
        Map<String, Object> nutritionCalories,
        Map<String, Object> nutritionWater,
        Map<String, Object> activityIntradaySteps,
        Map<String, Object> heartRateIntraday
) {
}
