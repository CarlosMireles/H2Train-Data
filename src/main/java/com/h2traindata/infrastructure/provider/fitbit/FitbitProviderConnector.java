package com.h2traindata.infrastructure.provider.fitbit;

import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.infrastructure.provider.fitbit.client.FitbitApiClient;
import com.h2traindata.infrastructure.provider.fitbit.config.FitbitProperties;
import com.h2traindata.infrastructure.provider.fitbit.dto.FitbitProfileDto;
import com.h2traindata.infrastructure.provider.fitbit.dto.FitbitTokenResponseDto;
import com.h2traindata.infrastructure.provider.fitbit.dto.FitbitUserDto;
import java.net.URI;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(prefix = "app.fitbit", name = "enabled", havingValue = "true")
public class FitbitProviderConnector implements ProviderConnector {

    private final FitbitProperties fitbitProperties;
    private final FitbitApiClient fitbitApiClient;

    public FitbitProviderConnector(FitbitProperties fitbitProperties, FitbitApiClient fitbitApiClient) {
        this.fitbitProperties = fitbitProperties;
        this.fitbitApiClient = fitbitApiClient;
    }

    @Override
    public String providerId() {
        return "fitbit";
    }

    @Override
    public URI buildAuthorizationUri() {
        return UriComponentsBuilder
                .fromUriString("https://www.fitbit.com/oauth2/authorize")
                .queryParam("client_id", fitbitProperties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", fitbitProperties.getRedirectUri())
                .queryParam("scope", String.join(" ", fitbitProperties.getScopes()))
                .build()
                .toUri();
    }

    @Override
    public ProviderConnection connect(String code) {
        FitbitTokenResponseDto tokenResponse = fitbitApiClient.exchangeCodeForToken(code);
        FitbitProfileDto profile = fitbitApiClient.fetchProfile(tokenResponse.accessToken());
        return toConnection(tokenResponse, profile);
    }

    @Override
    public ProviderConnection refresh(ProviderConnection connection) {
        if (!StringUtils.hasText(connection.refreshToken())) {
            return connection;
        }

        FitbitTokenResponseDto tokenResponse = fitbitApiClient.refreshAccessToken(connection.refreshToken());
        return new ProviderConnection(
                providerId(),
                connection.athlete(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                Instant.now().plusSeconds(tokenResponse.expiresIn())
        );
    }

    private ProviderConnection toConnection(FitbitTokenResponseDto tokenResponse, FitbitProfileDto profile) {
        FitbitUserDto user = profile != null ? profile.user() : null;
        if (user == null || !StringUtils.hasText(user.encodedId())) {
            throw new IllegalStateException("Fitbit did not return a valid user profile");
        }

        return new ProviderConnection(
                providerId(),
                new AthleteProfile(user.encodedId(), resolveUsername(user)),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                Instant.now().plusSeconds(tokenResponse.expiresIn())
        );
    }

    private String resolveUsername(FitbitUserDto user) {
        if (StringUtils.hasText(user.displayName())) {
            return user.displayName();
        }
        if (StringUtils.hasText(user.fullName())) {
            return user.fullName();
        }
        return user.encodedId();
    }
}
