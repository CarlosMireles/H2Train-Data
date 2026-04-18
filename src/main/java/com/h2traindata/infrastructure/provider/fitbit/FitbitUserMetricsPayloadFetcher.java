package com.h2traindata.infrastructure.provider.fitbit;

import static com.h2traindata.infrastructure.provider.common.PayloadSupport.mapValue;
import static com.h2traindata.infrastructure.provider.common.PayloadSupport.stringValue;
import static com.h2traindata.infrastructure.provider.common.ProviderRequestSupport.getOrDefault;

import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.infrastructure.provider.fitbit.client.FitbitApiClient;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "app.fitbit", name = "enabled", havingValue = "true")
class FitbitUserMetricsPayloadFetcher {

    private static final int HEALTH_METRICS_WINDOW_DAYS = 30;
    private static final int HEALTH_LIST_LIMIT = 1;

    private final FitbitApiClient fitbitApiClient;
    private final Executor metricsCollectorExecutor;

    FitbitUserMetricsPayloadFetcher(FitbitApiClient fitbitApiClient,
                                    @Qualifier("metricsCollectorExecutor") Executor metricsCollectorExecutor) {
        this.fitbitApiClient = fitbitApiClient;
        this.metricsCollectorExecutor = metricsCollectorExecutor;
    }

    FitbitUserMetricsPayloadBundle fetch(ProviderConnection connection) {
        String accessToken = connection.accessToken();
        CompletableFuture<Map<String, Object>> profilePayloadFuture =
                submitMap(() -> fitbitApiClient.fetchProfilePayload(accessToken));
        CompletableFuture<Map<String, Object>> lifetimeStatsFuture =
                submitMap(() -> fitbitApiClient.fetchLifetimeStats(accessToken));

        Map<String, Object> profilePayload = join(profilePayloadFuture);
        Map<String, Object> lifetimeStats = join(lifetimeStatsFuture);
        Map<String, Object> user = mapValue(profilePayload.get("user"));
        String athleteId = firstNonBlank(stringValue(user.get("encodedId")), connection.athlete().id());
        ZoneId zoneId = resolveZoneId(stringValue(user.get("timezone")));
        LocalDate endDate = LocalDate.now(zoneId);
        LocalDate startDate = endDate.minusDays(HEALTH_METRICS_WINDOW_DAYS - 1L);
        String beforeDate = endDate.plusDays(1L).toString();

        CompletableFuture<List<Map<String, Object>>> breathingRateFuture =
                submitList(() -> fitbitApiClient.fetchBreathingRateSummary(accessToken, startDate, endDate));
        CompletableFuture<List<Map<String, Object>>> vo2MaxFuture =
                submitList(() -> fitbitApiClient.fetchVo2MaxSummary(accessToken, startDate, endDate));
        CompletableFuture<List<Map<String, Object>>> bloodGlucoseFuture =
                submitList(() -> fitbitApiClient.fetchBloodGlucose(accessToken, startDate, endDate));
        CompletableFuture<Map<String, Object>> ecgFuture =
                submitMap(() -> fitbitApiClient.fetchEcgLogList(accessToken, beforeDate, null, HEALTH_LIST_LIMIT));
        CompletableFuture<Map<String, Object>> sleepFuture =
                submitMap(() -> fitbitApiClient.fetchSleepLogList(accessToken, beforeDate, null, HEALTH_LIST_LIMIT));
        CompletableFuture<List<Map<String, Object>>> spo2Future =
                submitList(() -> fitbitApiClient.fetchSpo2Summary(accessToken, startDate, endDate));
        CompletableFuture<List<Map<String, Object>>> hrvFuture =
                submitList(() -> fitbitApiClient.fetchHrvSummary(accessToken, startDate, endDate));
        CompletableFuture<List<Map<String, Object>>> temperatureSkinFuture =
                submitList(() -> fitbitApiClient.fetchTemperatureSkinSummary(accessToken, startDate, endDate));
        CompletableFuture<List<Map<String, Object>>> temperatureCoreFuture =
                submitList(() -> fitbitApiClient.fetchTemperatureCoreSummary(accessToken, startDate, endDate));
        CompletableFuture<Map<String, Object>> irnProfileFuture =
                submitMap(() -> fitbitApiClient.fetchIrnProfile(accessToken));
        CompletableFuture<Map<String, Object>> irnAlertsFuture =
                submitMap(() -> fitbitApiClient.fetchIrnAlerts(accessToken, beforeDate, null, HEALTH_LIST_LIMIT));

        FitbitHealthPayloads healthPayloads = new FitbitHealthPayloads(
                join(breathingRateFuture),
                join(vo2MaxFuture),
                join(bloodGlucoseFuture),
                join(ecgFuture),
                join(sleepFuture),
                join(spo2Future),
                join(hrvFuture),
                join(temperatureSkinFuture),
                join(temperatureCoreFuture),
                join(irnProfileFuture),
                join(irnAlertsFuture)
        );

        CompletableFuture<Map<String, Object>> activityGoalsDailyFuture =
                submitMap(() -> fitbitApiClient.fetchActivityGoals(accessToken, "daily"));
        CompletableFuture<Map<String, Object>> activityGoalsWeeklyFuture =
                submitMap(() -> fitbitApiClient.fetchActivityGoals(accessToken, "weekly"));
        CompletableFuture<Map<String, Object>> dailyActivitySummaryFuture =
                submitMap(() -> fitbitApiClient.fetchDailyActivitySummary(accessToken, endDate));
        CompletableFuture<Map<String, Object>> azmFuture =
                submitMap(() -> fitbitApiClient.fetchAzmTimeSeries(accessToken, startDate, endDate));
        CompletableFuture<Map<String, Object>> stepsTimeSeriesFuture =
                submitMap(() -> fitbitApiClient.fetchActivityTimeSeries(accessToken, "steps", startDate, endDate));
        CompletableFuture<Map<String, Object>> distanceTimeSeriesFuture =
                submitMap(() -> fitbitApiClient.fetchActivityTimeSeries(accessToken, "distance", startDate, endDate));
        CompletableFuture<Map<String, Object>> caloriesTimeSeriesFuture =
                submitMap(() -> fitbitApiClient.fetchActivityTimeSeries(accessToken, "calories", startDate, endDate));
        CompletableFuture<Map<String, Object>> weightLogFuture =
                submitMap(() -> fitbitApiClient.fetchWeightLog(accessToken, startDate, endDate));
        CompletableFuture<Map<String, Object>> bodyFatLogFuture =
                submitMap(() -> fitbitApiClient.fetchBodyFatLog(accessToken, startDate, endDate));
        CompletableFuture<Map<String, Object>> bodyWeightTimeSeriesFuture =
                submitMap(() -> fitbitApiClient.fetchBodyTimeSeries(accessToken, "weight", startDate, endDate));
        CompletableFuture<Map<String, Object>> bodyFatTimeSeriesFuture =
                submitMap(() -> fitbitApiClient.fetchBodyTimeSeries(accessToken, "fat", startDate, endDate));
        CompletableFuture<Map<String, Object>> bodyBmiTimeSeriesFuture =
                submitMap(() -> fitbitApiClient.fetchBodyTimeSeries(accessToken, "bmi", startDate, endDate));
        CompletableFuture<List<Map<String, Object>>> devicesFuture =
                submitList(() -> fitbitApiClient.fetchDevices(accessToken));
        CompletableFuture<Map<String, Object>> friendsFuture =
                submitMap(() -> fitbitApiClient.fetchFriends(accessToken));
        CompletableFuture<Map<String, Object>> friendsLeaderboardFuture =
                submitMap(() -> fitbitApiClient.fetchFriendsLeaderboard(accessToken));
        CompletableFuture<Map<String, Object>> heartRateTimeSeriesFuture =
                submitMap(() -> fitbitApiClient.fetchHeartRateTimeSeries(accessToken, startDate, endDate));
        CompletableFuture<Map<String, Object>> nutritionCaloriesFuture =
                submitMap(() -> fitbitApiClient.fetchNutritionTimeSeries(accessToken, "caloriesIn", startDate, endDate));
        CompletableFuture<Map<String, Object>> nutritionWaterFuture =
                submitMap(() -> fitbitApiClient.fetchNutritionTimeSeries(accessToken, "water", startDate, endDate));
        CompletableFuture<Map<String, Object>> activityIntradayStepsFuture =
                submitMap(() -> fitbitApiClient.fetchActivityIntraday(accessToken, "steps", endDate));
        CompletableFuture<Map<String, Object>> heartRateIntradayFuture =
                submitMap(() -> fitbitApiClient.fetchHeartRateIntraday(accessToken, endDate));

        FitbitSupplementalPayloads supplementalPayloads = new FitbitSupplementalPayloads(
                join(activityGoalsDailyFuture),
                join(activityGoalsWeeklyFuture),
                join(dailyActivitySummaryFuture),
                join(azmFuture),
                join(stepsTimeSeriesFuture),
                join(distanceTimeSeriesFuture),
                join(caloriesTimeSeriesFuture),
                join(weightLogFuture),
                join(bodyFatLogFuture),
                join(bodyWeightTimeSeriesFuture),
                join(bodyFatTimeSeriesFuture),
                join(bodyBmiTimeSeriesFuture),
                join(devicesFuture),
                join(friendsFuture),
                join(friendsLeaderboardFuture),
                join(heartRateTimeSeriesFuture),
                join(nutritionCaloriesFuture),
                join(nutritionWaterFuture),
                join(activityIntradayStepsFuture),
                join(heartRateIntradayFuture)
        );

        return new FitbitUserMetricsPayloadBundle(
                profilePayload,
                lifetimeStats,
                user,
                athleteId,
                startDate,
                endDate,
                healthPayloads,
                supplementalPayloads
        );
    }

    private CompletableFuture<Map<String, Object>> submitMap(Supplier<Map<String, Object>> supplier) {
        return CompletableFuture.supplyAsync(() -> getOrDefault(supplier, Map.of()), metricsCollectorExecutor);
    }

    private CompletableFuture<List<Map<String, Object>>> submitList(Supplier<List<Map<String, Object>>> supplier) {
        return CompletableFuture.supplyAsync(() -> getOrDefault(supplier, List.of()), metricsCollectorExecutor);
    }

    private <T> T join(CompletableFuture<T> future) {
        return future.join();
    }

    private ZoneId resolveZoneId(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ignored) {
            return ZoneId.of("UTC");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
