package com.stravatft.client;

import com.stravatft.config.StravaProperties;
import com.stravatft.web.dto.StravaActivity;
import com.stravatft.web.dto.StravaTokenResponse;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class StravaApiClient {

    private static final String STRAVA_BASE_URL = "https://www.strava.com/api/v3";
    private static final String STRAVA_OAUTH_URL = "https://www.strava.com/oauth";

    private final RestClient restClient;
    private final StravaProperties stravaProperties;

    public StravaApiClient(StravaProperties stravaProperties) {
        this.stravaProperties = stravaProperties;
        this.restClient = RestClient.builder().build();
    }

    public StravaTokenResponse exchangeCodeForToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", stravaProperties.getClientId());
        form.add("client_secret", stravaProperties.getClientSecret());
        form.add("code", code);
        form.add("grant_type", "authorization_code");

        return restClient.post()
                .uri(STRAVA_OAUTH_URL + "/token")
                .body(form)
                .retrieve()
                .body(StravaTokenResponse.class);
    }

    public List<StravaActivity> fetchActivities(String accessToken, int pageSize) {
        return restClient.get()
                .uri(STRAVA_BASE_URL + "/athlete/activities?per_page=" + pageSize)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
