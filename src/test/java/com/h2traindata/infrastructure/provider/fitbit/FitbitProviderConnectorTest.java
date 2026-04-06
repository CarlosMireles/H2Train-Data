package com.h2traindata.infrastructure.provider.fitbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.infrastructure.provider.fitbit.client.FitbitApiClient;
import com.h2traindata.infrastructure.provider.fitbit.config.FitbitProperties;
import com.h2traindata.infrastructure.provider.fitbit.dto.FitbitProfileDto;
import com.h2traindata.infrastructure.provider.fitbit.dto.FitbitTokenResponseDto;
import com.h2traindata.infrastructure.provider.fitbit.dto.FitbitUserDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FitbitProviderConnectorTest {

    private final FitbitApiClient fitbitApiClient = Mockito.mock(FitbitApiClient.class);
    private final FitbitProviderConnector provider =
            new FitbitProviderConnector(fitbitProperties(), fitbitApiClient);

    @Test
    void connectBuildsConnectionFromProfileAndToken() {
        when(fitbitApiClient.exchangeCodeForToken("fitbit-code")).thenReturn(new FitbitTokenResponseDto(
                "fitbit-access-token",
                "fitbit-refresh-token",
                28800L,
                "Bearer",
                "activity profile"
        ));
        when(fitbitApiClient.fetchProfile("fitbit-access-token")).thenReturn(new FitbitProfileDto(
                new FitbitUserDto("ABC123", "fitbit-runner", "Fitbit Runner")
        ));

        ProviderConnection connection = provider.connect("fitbit-code");

        assertEquals("ABC123", connection.athlete().id());
        assertEquals("fitbit-runner", connection.athlete().username());
        assertEquals("fitbit-access-token", connection.accessToken());
        assertTrue(connection.expiresAt().isAfter(java.time.Instant.now()));
    }

    private FitbitProperties fitbitProperties() {
        FitbitProperties properties = new FitbitProperties();
        properties.setEnabled(true);
        properties.setClientId("fitbit-client");
        properties.setClientSecret("fitbit-secret");
        properties.setRedirectUri("http://localhost:8080/auth/fitbit/callback");
        return properties;
    }
}
