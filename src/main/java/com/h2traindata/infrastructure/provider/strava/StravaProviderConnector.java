package com.h2traindata.infrastructure.provider.strava;

import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.infrastructure.provider.strava.client.StravaApiClient;
import com.h2traindata.infrastructure.provider.strava.config.StravaProperties;
import com.h2traindata.infrastructure.provider.strava.dto.StravaAthleteDto;
import com.h2traindata.infrastructure.provider.strava.dto.StravaTokenResponseDto;
import java.net.URI;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class StravaProviderConnector implements ProviderConnector {

    private final StravaProperties stravaProperties;
    private final StravaApiClient stravaApiClient;

    public StravaProviderConnector(StravaProperties stravaProperties, StravaApiClient stravaApiClient) {
        this.stravaProperties = stravaProperties;
        this.stravaApiClient = stravaApiClient;
    }

    @Override
    public String providerId() {
        return "strava";
    }

    @Override
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

    @Override
    public ProviderConnection connect(String code) {
        StravaTokenResponseDto tokenResponse = stravaApiClient.exchangeCodeForToken(code);
        StravaAthleteDto athlete = resolveAthleteProfile(tokenResponse);
        return toConnection(tokenResponse, athlete);
    }

    @Override
    public ProviderConnection refresh(ProviderConnection connection) {
        if (!StringUtils.hasText(connection.refreshToken())) {
            return connection;
        }

        StravaTokenResponseDto tokenResponse = stravaApiClient.refreshAccessToken(connection.refreshToken());
        return new ProviderConnection(
                providerId(),
                connection.athlete(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                Instant.ofEpochSecond(tokenResponse.expiresAt())
        );
    }

    private ProviderConnection toConnection(StravaTokenResponseDto tokenResponse, StravaAthleteDto athlete) {
        return new ProviderConnection(
                providerId(),
                new AthleteProfile(resolveAthleteId(athlete), resolveUsername(athlete)),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                Instant.ofEpochSecond(tokenResponse.expiresAt())
        );
    }

    private StravaAthleteDto resolveAthleteProfile(StravaTokenResponseDto tokenResponse) {
        StravaAthleteDto tokenAthlete = tokenResponse.athlete();
        StravaAthleteDto profileAthlete = stravaApiClient.fetchAthlete(tokenResponse.accessToken());
        if (profileAthlete == null) {
            return tokenAthlete;
        }
        return new StravaAthleteDto(
                profileAthlete.id() != null ? profileAthlete.id() : athleteId(tokenAthlete),
                firstNonBlank(profileAthlete.username(), tokenAthlete != null ? tokenAthlete.username() : null),
                firstNonBlank(profileAthlete.firstname(), tokenAthlete != null ? tokenAthlete.firstname() : null),
                firstNonBlank(profileAthlete.lastname(), tokenAthlete != null ? tokenAthlete.lastname() : null)
        );
    }

    private String resolveAthleteId(StravaAthleteDto athlete) {
        if (athlete == null || athlete.id() == null) {
            throw new IllegalStateException("Strava did not return an athlete id");
        }
        return String.valueOf(athlete.id());
    }

    private String resolveUsername(StravaAthleteDto athlete) {
        String username = firstNonBlank(athlete.username(), fullName(athlete));
        return StringUtils.hasText(username) ? username : null;
    }

    private String fullName(StravaAthleteDto athlete) {
        String firstname = athlete.firstname();
        String lastname = athlete.lastname();
        if (!StringUtils.hasText(firstname) && !StringUtils.hasText(lastname)) {
            return null;
        }
        if (!StringUtils.hasText(firstname)) {
            return lastname.trim();
        }
        if (!StringUtils.hasText(lastname)) {
            return firstname.trim();
        }
        return firstname.trim() + " " + lastname.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private Long athleteId(StravaAthleteDto athlete) {
        return athlete != null ? athlete.id() : null;
    }
}
