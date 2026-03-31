package com.stravatft.service;

import com.stravatft.client.StravaApiClient;
import com.stravatft.config.StravaProperties;
import com.stravatft.web.dto.StravaTokenResponse;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class StravaConnectionService {

    private final StravaProperties stravaProperties;
    private final StravaApiClient stravaApiClient;
    private final Map<Long, StoredAthleteConnection> connections = new ConcurrentHashMap<>();

    public StravaConnectionService(StravaProperties stravaProperties, StravaApiClient stravaApiClient) {
        this.stravaProperties = stravaProperties;
        this.stravaApiClient = stravaApiClient;
    }

    public URI buildAuthorizationUri() {
        return UriComponentsBuilder
                .fromUriString("https://www.strava.com/oauth/authorize")
                .queryParam("client_id", stravaProperties.getClientId())
                .queryParam("redirect_uri", stravaProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("approval_prompt", "auto")
                .queryParam("scope", String.join(",", stravaProperties.getScopes()))
                .build()
                .toUri();
    }

    public StoredAthleteConnection connectAthlete(String code) {
        StravaTokenResponse tokenResponse = stravaApiClient.exchangeCodeForToken(code);
        StoredAthleteConnection connection = new StoredAthleteConnection(
                tokenResponse.athlete().id(),
                tokenResponse.athlete().username(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                Instant.ofEpochSecond(tokenResponse.expiresAt())
        );
        connections.put(connection.athleteId(), connection);
        return connection;
    }

    public record StoredAthleteConnection(
            long athleteId,
            String username,
            String accessToken,
            String refreshToken,
            Instant expiresAt
    ) {
    }
}
