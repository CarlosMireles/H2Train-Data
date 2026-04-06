package com.h2traindata.infrastructure.provider.strava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.infrastructure.provider.strava.client.StravaApiClient;
import com.h2traindata.infrastructure.provider.strava.config.StravaProperties;
import com.h2traindata.infrastructure.provider.strava.dto.StravaAthleteDto;
import com.h2traindata.infrastructure.provider.strava.dto.StravaTokenResponseDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class StravaProviderConnectorTest {

    private final StravaApiClient stravaApiClient = Mockito.mock(StravaApiClient.class);
    private final StravaProviderConnector provider =
            new StravaProviderConnector(stravaProperties(), stravaApiClient);

    @Test
    void connectUsesCanonicalAthleteProfileAndFallsBackToFullName() {
        when(stravaApiClient.exchangeCodeForToken("test-code")).thenReturn(new StravaTokenResponseDto(
                "Bearer",
                "access-token",
                1_700_000_000L,
                2_1600L,
                "refresh-token",
                new StravaAthleteDto(11L, null, "Token", "Only")
        ));
        when(stravaApiClient.fetchAthlete("access-token")).thenReturn(new StravaAthleteDto(
                22L,
                null,
                "Carlos",
                "Runner"
        ));

        ProviderConnection connection = provider.connect("test-code");

        assertEquals("22", connection.athlete().id());
        assertEquals("Carlos Runner", connection.athlete().username());
    }

    @Test
    void connectFallsBackToOauthAthleteWhenProfileUsernameFieldsAreMissing() {
        when(stravaApiClient.exchangeCodeForToken("test-code")).thenReturn(new StravaTokenResponseDto(
                "Bearer",
                "access-token",
                1_700_000_000L,
                2_1600L,
                "refresh-token",
                new StravaAthleteDto(33L, "token-user", null, null)
        ));
        when(stravaApiClient.fetchAthlete("access-token")).thenReturn(new StravaAthleteDto(
                null,
                null,
                null,
                null
        ));

        ProviderConnection connection = provider.connect("test-code");

        assertEquals("33", connection.athlete().id());
        assertEquals("token-user", connection.athlete().username());
    }

    private StravaProperties stravaProperties() {
        StravaProperties properties = new StravaProperties();
        properties.setClientId("12345");
        properties.setClientSecret("test-secret");
        properties.setRedirectUri("http://localhost:8080/auth/strava/callback");
        return properties;
    }
}
