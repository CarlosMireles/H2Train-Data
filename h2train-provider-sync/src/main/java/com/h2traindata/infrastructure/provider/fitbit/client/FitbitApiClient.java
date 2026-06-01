package com.h2traindata.infrastructure.provider.fitbit.client;

import static com.h2traindata.infrastructure.provider.common.ProviderRequestSupport.execute;

import com.h2traindata.infrastructure.provider.fitbit.config.FitbitProperties;
import com.h2traindata.infrastructure.provider.fitbit.dto.FitbitProfileDto;
import com.h2traindata.infrastructure.provider.fitbit.dto.FitbitTokenResponseDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(prefix = "app.fitbit", name = "enabled", havingValue = "true")
public class FitbitApiClient {

    private static final String PROVIDER_ID = "fitbit";
    private static final String FITBIT_API_URL = "https://api.fitbit.com";
    private static final String FITBIT_OAUTH_URL = FITBIT_API_URL + "/oauth2";

    private final RestClient restClient;
    private final FitbitProperties fitbitProperties;

    public FitbitApiClient(FitbitProperties fitbitProperties, RestClient providerRestClient) {
        this.fitbitProperties = fitbitProperties;
        this.restClient = providerRestClient;
    }

    public FitbitTokenResponseDto exchangeCodeForToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", fitbitProperties.getClientId());
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", fitbitProperties.getRedirectUri());
        form.add("code", code);

        return execute(PROVIDER_ID, "exchange an authorization code for a token", () -> restClient.post()
                .uri(FITBIT_OAUTH_URL + "/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setBasicAuth(fitbitProperties.getClientId(), fitbitProperties.getClientSecret()))
                .body(form)
                .retrieve()
                .body(FitbitTokenResponseDto.class));
    }

    public FitbitTokenResponseDto refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        return execute(PROVIDER_ID, "refresh an access token", () -> restClient.post()
                .uri(FITBIT_OAUTH_URL + "/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setBasicAuth(fitbitProperties.getClientId(), fitbitProperties.getClientSecret()))
                .body(form)
                .retrieve()
                .body(FitbitTokenResponseDto.class));
    }

    public FitbitProfileDto fetchProfile(String accessToken) {
        return execute(PROVIDER_ID, "fetch the Fitbit profile", () -> restClient.get()
                .uri(FITBIT_API_URL + "/1/user/-/profile.json")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(FitbitProfileDto.class));
    }

    public Map<String, Object> fetchProfilePayload(String accessToken) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/profile.json");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchActivityLogs(String accessToken, String afterDate, String beforeDate, int limit) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(FITBIT_API_URL + "/1/user/-/activities/list.json")
                .queryParam("offset", 0)
                .queryParam("limit", limit);

        if (afterDate != null) {
            uriBuilder.queryParam("afterDate", afterDate).queryParam("sort", "asc");
        } else {
            uriBuilder.queryParam("beforeDate", beforeDate).queryParam("sort", "desc");
        }

        Map<String, Object> response = execute(PROVIDER_ID, "fetch Fitbit activity logs", () -> restClient.get()
                .uri(uriBuilder.toUriString())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));

        Object activities = response != null ? response.get("activities") : null;
        return activities instanceof List<?> list
                ? list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList()
                : List.of();
    }

    public String fetchActivityTcx(String accessToken, long logId) {
        return execute(PROVIDER_ID, "fetch Fitbit TCX activity details", () -> restClient.get()
                .uri(UriComponentsBuilder
                        .fromUriString(FITBIT_API_URL + "/1/user/-/activities/" + logId + ".tcx")
                        .queryParam("includePartialTCX", true)
                        .toUriString())
                .headers(headers -> {
                    headers.setBearerAuth(accessToken);
                    headers.setAccept(List.of(MediaType.APPLICATION_XML));
                })
                .retrieve()
                .body(String.class));
    }

    public Map<String, Object> fetchWorkoutSummary(String accessToken, long logId) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities/" + logId + "/workout-summary.json");
    }

    public Map<String, Object> fetchLifetimeStats(String accessToken) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities.json");
    }

    public Map<String, Object> fetchActivityGoals(String accessToken, String period) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities/goals/" + period + ".json");
    }

    public Map<String, Object> fetchDailyActivitySummary(String accessToken, LocalDate date) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities/date/" + date + ".json");
    }

    public Map<String, Object> fetchActivityTimeSeries(String accessToken, String resourcePath, LocalDate startDate, LocalDate endDate) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities/" + resourcePath + "/date/" + startDate + "/" + endDate + ".json");
    }

    public Map<String, Object> fetchAzmTimeSeries(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities/active-zone-minutes/date/" + startDate + "/" + endDate + ".json");
    }

    public Map<String, Object> fetchWeightLog(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/body/log/weight/date/" + startDate + "/" + endDate + ".json");
    }

    public Map<String, Object> fetchBodyFatLog(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/body/log/fat/date/" + startDate + "/" + endDate + ".json");
    }

    public Map<String, Object> fetchBodyTimeSeries(String accessToken, String resource, LocalDate startDate, LocalDate endDate) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/body/" + resource + "/date/" + startDate + "/" + endDate + ".json");
    }

    public List<Map<String, Object>> fetchDevices(String accessToken) {
        return getList(accessToken, FITBIT_API_URL + "/1/user/-/devices.json");
    }

    public Map<String, Object> fetchFriends(String accessToken) {
        return getMap(accessToken, FITBIT_API_URL + "/1.1/user/-/friends.json");
    }

    public Map<String, Object> fetchFriendsLeaderboard(String accessToken) {
        return getMap(accessToken, FITBIT_API_URL + "/1.1/user/-/leaderboard/friends.json");
    }

    public Map<String, Object> fetchHeartRateTimeSeries(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities/heart/date/" + startDate + "/" + endDate + ".json");
    }

    public Map<String, Object> fetchActivityIntraday(String accessToken, String resource, LocalDate date) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities/" + resource + "/date/" + date + "/" + date + "/1min.json");
    }

    public Map<String, Object> fetchHeartRateIntraday(String accessToken, LocalDate date) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/activities/heart/date/" + date + "/" + date + "/1min.json");
    }

    public Map<String, Object> fetchNutritionTimeSeries(String accessToken, String resource, LocalDate startDate, LocalDate endDate) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/foods/log/" + resource + "/date/" + startDate + "/" + endDate + ".json");
    }

    public List<Map<String, Object>> fetchBreathingRateSummary(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getList(accessToken, FITBIT_API_URL + "/1/user/-/br/date/" + startDate + "/" + endDate + ".json");
    }

    public List<Map<String, Object>> fetchVo2MaxSummary(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getList(accessToken, FITBIT_API_URL + "/1/user/-/cardioscore/date/" + startDate + "/" + endDate + ".json");
    }

    public List<Map<String, Object>> fetchBloodGlucose(String accessToken, LocalDate startDate, LocalDate endDate) {
        return execute(PROVIDER_ID, "fetch Fitbit blood glucose values", () -> restClient.get()
                .uri(FITBIT_API_URL + "/1/user/-/health/metrics/glucose/values/" + startDate + "/" + endDate + ".json")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }));
    }

    public Map<String, Object> fetchEcgLogList(String accessToken, String beforeDate, String afterDate, int limit) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(FITBIT_API_URL + "/1/user/-/ecg/list.json")
                .queryParam("offset", 0)
                .queryParam("limit", limit);

        if (beforeDate != null) {
            uriBuilder.queryParam("beforeDate", beforeDate).queryParam("sort", "desc");
        } else {
            uriBuilder.queryParam("afterDate", afterDate).queryParam("sort", "asc");
        }

        return getMap(accessToken, uriBuilder.toUriString());
    }

    public Map<String, Object> fetchSleepLogList(String accessToken, String beforeDate, String afterDate, int limit) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(FITBIT_API_URL + "/1.2/user/-/sleep/list.json")
                .queryParam("offset", 0)
                .queryParam("limit", limit);

        if (beforeDate != null) {
            uriBuilder.queryParam("beforeDate", beforeDate).queryParam("sort", "desc");
        } else {
            uriBuilder.queryParam("afterDate", afterDate).queryParam("sort", "asc");
        }

        return getMap(accessToken, uriBuilder.toUriString());
    }

    public List<Map<String, Object>> fetchSpo2Summary(String accessToken, LocalDate startDate, LocalDate endDate) {
        return execute(PROVIDER_ID, "fetch Fitbit SpO2 summaries", () -> restClient.get()
                .uri(FITBIT_API_URL + "/1/user/-/spo2/date/" + startDate + "/" + endDate + ".json")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }));
    }

    public List<Map<String, Object>> fetchHrvSummary(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getList(accessToken, FITBIT_API_URL + "/1/user/-/hrv/date/" + startDate + "/" + endDate + ".json");
    }

    public List<Map<String, Object>> fetchTemperatureSkinSummary(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getList(accessToken, FITBIT_API_URL + "/1/user/-/temp/skin/date/" + startDate + "/" + endDate + ".json");
    }

    public List<Map<String, Object>> fetchTemperatureCoreSummary(String accessToken, LocalDate startDate, LocalDate endDate) {
        return getList(accessToken, FITBIT_API_URL + "/1/user/-/temp/core/date/" + startDate + "/" + endDate + ".json");
    }

    public Map<String, Object> fetchIrnProfile(String accessToken) {
        return getMap(accessToken, FITBIT_API_URL + "/1/user/-/irn/profile.json");
    }

    public Map<String, Object> fetchIrnAlerts(String accessToken, String beforeDate, String afterDate, int limit) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(FITBIT_API_URL + "/1/user/-/irn/alerts/list.json")
                .queryParam("offset", 0)
                .queryParam("limit", limit);

        if (beforeDate != null) {
            uriBuilder.queryParam("beforeDate", beforeDate).queryParam("sort", "desc");
        } else {
            uriBuilder.queryParam("afterDate", afterDate).queryParam("sort", "asc");
        }

        return getMap(accessToken, uriBuilder.toUriString());
    }

    private Map<String, Object> getMap(String accessToken, String uri) {
        return execute(PROVIDER_ID, "fetch Fitbit data from " + uri, () -> restClient.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
    }

    private List<Map<String, Object>> getList(String accessToken, String uri) {
        return execute(PROVIDER_ID, "fetch Fitbit data from " + uri, () -> restClient.get()
                .uri(uri)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }));
    }
}
