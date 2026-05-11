package com.h2traindata.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.ProviderConnection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HandleAuthorizationCallbackUseCaseTest {

    private final ProviderConnector connector = Mockito.mock(ProviderConnector.class);
    private final ProviderEventCollector collector = Mockito.mock(ProviderEventCollector.class);
    private final ConnectionRepository connectionRepository = Mockito.mock(ConnectionRepository.class);
    private final UserAccountRepository userAccountRepository = Mockito.mock(UserAccountRepository.class);

    @Test
    void linksNewProviderConnectionToExistingInternalUser() {
        when(connector.providerId()).thenReturn("fitbit");
        when(collector.providerId()).thenReturn("fitbit");
        when(collector.eventType()).thenReturn(EventType.HEALTH);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        HandleAuthorizationCallbackUseCase useCase = new HandleAuthorizationCallbackUseCase(
                providerRegistry,
                connectionRepository,
                userAccountRepository
        );

        ProviderConnection providerConnection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("ABC123", "fitbit-runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600)
        );
        when(connector.connect("oauth-code")).thenReturn(providerConnection);
        when(userAccountRepository.findById("internal-user-1"))
                .thenReturn(Optional.of(new InternalUserAccount("internal-user-1", Instant.parse("2026-04-01T10:00:00Z"))));

        ProviderConnection linkedConnection = useCase.execute("fitbit", "oauth-code", "internal-user-1");

        assertEquals("internal-user-1", linkedConnection.userId());
        verify(connectionRepository).save(linkedConnection);
    }

    @Test
    void createsInternalUserWhenProviderConnectionIsFirstIdentity() {
        when(connector.providerId()).thenReturn("strava");
        when(collector.providerId()).thenReturn("strava");
        when(collector.eventType()).thenReturn(EventType.USER_STATE);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        HandleAuthorizationCallbackUseCase useCase = new HandleAuthorizationCallbackUseCase(
                providerRegistry,
                connectionRepository,
                userAccountRepository
        );

        ProviderConnection providerConnection = new ProviderConnection(
                "strava",
                new AthleteProfile("99", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600)
        );
        when(connector.connect("oauth-code")).thenReturn(providerConnection);
        when(userAccountRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderConnection linkedConnection = useCase.execute("strava", "oauth-code", null);

        assertNotNull(linkedConnection.userId());
        verify(connectionRepository).save(linkedConnection);
    }
}
