package com.h2traindata.infrastructure.provider.fitbit.client;

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

    private static final String FITBIT_API_URL = "https://api.fitbit.com";
    private static final String FITBIT_OAUTH_URL = FITBIT_API_URL + "/oauth2";

    private final RestClient restClient;
    private final FitbitProperties fitbitProperties;

    public FitbitApiClient(FitbitProperties fitbitProperties) {
        this.fitbitProperties = fitbitProperties;
        this.restClient = RestClient.builder().build();
    }

    public FitbitTokenResponseDto exchangeCodeForToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", fitbitProperties.getClientId());
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", fitbitProperties.getRedirectUri());
        form.add("code", code);

        return restClient.post()
                .uri(FITBIT_OAUTH_URL + "/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setBasicAuth(fitbitProperties.getClientId(), fitbitProperties.getClientSecret()))
                .body(form)
                .retrieve()
                .body(FitbitTokenResponseDto.class);
    }

    public FitbitTokenResponseDto refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        return restClient.post()
                .uri(FITBIT_OAUTH_URL + "/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> headers.setBasicAuth(fitbitProperties.getClientId(), fitbitProperties.getClientSecret()))
                .body(form)
                .retrieve()
                .body(FitbitTokenResponseDto.class);
    }

    public FitbitProfileDto fetchProfile(String accessToken) {
        return restClient.get()
                .uri(FITBIT_API_URL + "/1/user/-/profile.json")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(FitbitProfileDto.class);
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

        Map<String, Object> response = restClient.get()
                .uri(uriBuilder.toUriString())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        Object activities = response != null ? response.get("activities") : null;
        return activities instanceof List<?> list
                ? list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList()
                : List.of();
    }

    public String fetchActivityTcx(String accessToken, long logId) {
        return restClient.get()
                .uri(UriComponentsBuilder
                        .fromUriString(FITBIT_API_URL + "/1/user/-/activities/" + logId + ".tcx")
                        .queryParam("includePartialTCX", true)
                        .toUriString())
                .headers(headers -> {
                    headers.setBearerAuth(accessToken);
                    headers.setAccept(List.of(MediaType.APPLICATION_XML));
                })
                .retrieve()
                .body(String.class);
    }

    public Map<String, Object> fetchWorkoutSummary(String accessToken, long logId) {
        return restClient.get()
                .uri(FITBIT_API_URL + "/1/user/-/activities/" + logId + "/workout-summary.json")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }
}
