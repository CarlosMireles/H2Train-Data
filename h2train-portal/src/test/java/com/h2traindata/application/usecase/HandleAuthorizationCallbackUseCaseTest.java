package com.h2traindata.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.h2traindata.application.exception.AuthenticationRequiredException;
import com.h2traindata.application.exception.ProviderConnectionAlreadyLinkedException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncCursor;
import com.h2traindata.domain.SyncInterval;
import com.h2traindata.domain.SyncPreferences;
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
    private final AccountEventPublisher accountEventPublisher = Mockito.mock(AccountEventPublisher.class);

    @Test
    void linksNewProviderConnectionToExistingInternalUser() {
        when(connector.providerId()).thenReturn("fitbit");
        when(collector.providerId()).thenReturn("fitbit");
        when(collector.eventType()).thenReturn(EventType.HEALTH);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        HandleAuthorizationCallbackUseCase useCase = new HandleAuthorizationCallbackUseCase(
                providerRegistry,
                connectionRepository,
                userAccountRepository,
                accountEventPublisher
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
        verify(accountEventPublisher).publishProviderAccountSynced(
                new InternalUserAccount("internal-user-1", Instant.parse("2026-04-01T10:00:00Z")).withProvider("fitbit"),
                linkedConnection
        );
    }

    @Test
    void rejectsProviderConnectionAlreadyLinkedToAnotherInternalUser() {
        when(connector.providerId()).thenReturn("fitbit");
        when(collector.providerId()).thenReturn("fitbit");
        when(collector.eventType()).thenReturn(EventType.HEALTH);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        HandleAuthorizationCallbackUseCase useCase = new HandleAuthorizationCallbackUseCase(
                providerRegistry,
                connectionRepository,
                userAccountRepository,
                accountEventPublisher
        );

        ProviderConnection providerConnection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("ABC123", "fitbit-runner"),
                "new-access-token",
                "new-refresh-token",
                Instant.parse("2026-04-10T10:00:00Z")
        );
        ProviderConnection existingConnection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("ABC123", "fitbit-runner"),
                "old-access-token",
                "old-refresh-token",
                Instant.parse("2026-04-09T10:00:00Z")
        ).withUserId("internal-user-2");

        when(connector.connect("oauth-code")).thenReturn(providerConnection);
        when(userAccountRepository.findById("internal-user-1"))
                .thenReturn(Optional.of(new InternalUserAccount("internal-user-1", Instant.parse("2026-04-01T10:00:00Z"))));
        when(connectionRepository.findByProviderAndAthlete("fitbit", "ABC123"))
                .thenReturn(Optional.of(existingConnection));

        assertThrows(
                ProviderConnectionAlreadyLinkedException.class,
                () -> useCase.execute("fitbit", "oauth-code", "internal-user-1")
        );

        verify(connectionRepository, never()).save(Mockito.any());
        verify(accountEventPublisher, never()).publishProviderAccountSynced(Mockito.any(), Mockito.any());
    }

    @Test
    void reconnectingSameProviderAccountPreservesExistingSyncSettingsAndUserAssociation() {
        when(connector.providerId()).thenReturn("strava");
        when(collector.providerId()).thenReturn("strava");
        when(collector.eventType()).thenReturn(EventType.ACTIVITY);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        HandleAuthorizationCallbackUseCase useCase = new HandleAuthorizationCallbackUseCase(
                providerRegistry,
                connectionRepository,
                userAccountRepository,
                accountEventPublisher
        );

        ProviderConnection refreshedProviderConnection = new ProviderConnection(
                "strava",
                new AthleteProfile("99", "runner-updated"),
                "new-access-token",
                "new-refresh-token",
                Instant.parse("2026-04-10T10:00:00Z")
        );
        ProviderConnection existingConnection = new ProviderConnection(
                "strava",
                new AthleteProfile("99", "runner"),
                "old-access-token",
                "old-refresh-token",
                Instant.parse("2026-04-09T10:00:00Z"),
                new SyncPreferences(false, SyncInterval.EVERY_7_DAYS),
                new SyncCursor("cursor-1"),
                Instant.parse("2026-04-08T10:00:00Z"),
                "internal-user-1"
        );

        when(connector.connect("oauth-code")).thenReturn(refreshedProviderConnection);
        when(userAccountRepository.findById("internal-user-1"))
                .thenReturn(Optional.of(new InternalUserAccount("internal-user-1", Instant.parse("2026-04-01T10:00:00Z"))));
        when(connectionRepository.findByProviderAndAthlete("strava", "99"))
                .thenReturn(Optional.of(existingConnection));

        ProviderConnection linkedConnection = useCase.execute("strava", "oauth-code", "internal-user-1");

        assertEquals("internal-user-1", linkedConnection.userId());
        assertEquals("new-access-token", linkedConnection.accessToken());
        assertEquals("new-refresh-token", linkedConnection.refreshToken());
        assertEquals("runner-updated", linkedConnection.athlete().username());
        assertEquals(false, linkedConnection.syncPreferences().enabled());
        assertEquals(SyncInterval.EVERY_7_DAYS, linkedConnection.syncPreferences().interval());
        assertEquals("cursor-1", linkedConnection.lastSyncCursor().value());
        assertEquals(Instant.parse("2026-04-08T10:00:00Z"), linkedConnection.lastSyncedAt());
        verify(connectionRepository).save(linkedConnection);
    }

    @Test
    void rejectsProviderConnectionWithoutInternalUser() {
        when(connector.providerId()).thenReturn("strava");
        when(collector.providerId()).thenReturn("strava");
        when(collector.eventType()).thenReturn(EventType.USER_STATE);

        ProviderRegistry providerRegistry = new ProviderRegistry(List.of(connector), List.of(collector));
        HandleAuthorizationCallbackUseCase useCase = new HandleAuthorizationCallbackUseCase(
                providerRegistry,
                connectionRepository,
                userAccountRepository,
                accountEventPublisher
        );

        ProviderConnection providerConnection = new ProviderConnection(
                "strava",
                new AthleteProfile("99", "runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600)
        );
        when(connector.connect("oauth-code")).thenReturn(providerConnection);
        assertThrows(AuthenticationRequiredException.class, () -> useCase.execute("strava", "oauth-code", null));
    }
}
