package com.h2traindata.infrastructure.provider.strava.client;

import static com.h2traindata.infrastructure.provider.common.ProviderRequestSupport.execute;

import com.h2traindata.infrastructure.provider.strava.config.StravaProperties;
import com.h2traindata.infrastructure.provider.strava.dto.StravaAthleteDto;
import com.h2traindata.infrastructure.provider.strava.dto.StravaTokenResponseDto;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class StravaApiClient {

    private static final String PROVIDER_ID = "strava";
    private static final String STRAVA_BASE_URL = "https://www.strava.com/api/v3";
    private static final String STRAVA_OAUTH_URL = "https://www.strava.com/oauth";
    private static final List<String> STREAM_KEYS = List.of(
            "time",
            "distance",
            "latlng",
            "altitude",
            "velocity_smooth",
            "heartrate",
            "cadence",
            "watts",
            "temp",
            "moving",
            "grade_smooth"
    );

    private final RestClient restClient;
    private final StravaProperties stravaProperties;

    public StravaApiClient(StravaProperties stravaProperties, RestClient providerRestClient) {
        this.stravaProperties = stravaProperties;
        this.restClient = providerRestClient;
    }

    public StravaTokenResponseDto exchangeCodeForToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", stravaProperties.getClientId());
        form.add("client_secret", stravaProperties.getClientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");

        return execute(PROVIDER_ID, "exchange an authorization code for a token", () -> restClient.post()
                .uri(STRAVA_OAUTH_URL + "/token")
                .body(form)
                .retrieve()
                .body(StravaTokenResponseDto.class));
    }

    public StravaTokenResponseDto refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", stravaProperties.getClientId());
        form.add("client_secret", stravaProperties.getClientSecret());
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        return execute(PROVIDER_ID, "refresh an access token", () -> restClient.post()
                .uri(STRAVA_OAUTH_URL + "/token")
                .body(form)
                .retrieve()
                .body(StravaTokenResponseDto.class));
    }

    public StravaAthleteDto fetchAthlete(String accessToken) {
        return execute(PROVIDER_ID, "fetch the Strava athlete profile", () -> restClient.get()
                .uri(STRAVA_BASE_URL + "/athlete")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(StravaAthleteDto.class));
    }

    public Map<String, Object> fetchAthletePayload(String accessToken) {
        return execute(PROVIDER_ID, "fetch the Strava athlete payload", () -> restClient.get()
                .uri(STRAVA_BASE_URL + "/athlete")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
    }

    public List<Map<String, Object>> fetchActivities(String accessToken, int pageSize, Long afterEpoch) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(STRAVA_BASE_URL + "/athlete/activities")
                .queryParam("per_page", pageSize);
        if (afterEpoch != null) {
            uriBuilder.queryParam("after", afterEpoch);
        }

        return execute(PROVIDER_ID, "fetch Strava activities", () -> restClient.get()
                .uri(uriBuilder.toUriString())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }));
    }

    public Map<String, Object> fetchActivity(String accessToken, long activityId) {
        return execute(PROVIDER_ID, "fetch a Strava activity", () -> restClient.get()
                .uri(UriComponentsBuilder
                        .fromUriString(STRAVA_BASE_URL + "/activities/" + activityId)
                        .queryParam("include_all_efforts", true)
                        .toUriString())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
    }

    public Map<String, Object> fetchActivityStreams(String accessToken, long activityId) {
        return execute(PROVIDER_ID, "fetch Strava activity streams", () -> restClient.get()
                .uri(UriComponentsBuilder
                        .fromUriString(STRAVA_BASE_URL + "/activities/" + activityId + "/streams")
                        .queryParam("keys", String.join(",", STREAM_KEYS))
                        .queryParam("key_by_type", true)
                        .toUriString())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
    }

    public List<Map<String, Object>> fetchActivityZones(String accessToken, long activityId) {
        List<Map<String, Object>> zones = execute(PROVIDER_ID, "fetch Strava activity zones", () -> restClient.get()
                .uri(STRAVA_BASE_URL + "/activities/" + activityId + "/zones")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }));
        return zones == null ? List.of() : zones;
    }

    public Map<String, Object> fetchAthleteStats(String accessToken, long athleteId) {
        return execute(PROVIDER_ID, "fetch Strava athlete stats", () -> restClient.get()
                .uri(STRAVA_BASE_URL + "/athletes/" + athleteId + "/stats")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                }));
    }
}
