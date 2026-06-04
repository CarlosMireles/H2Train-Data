package com.h2traindata.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.h2traindata.application.exception.ProviderConnectionAlreadyLinkedException;
import com.h2traindata.application.exception.ProviderRateLimitException;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.application.usecase.GetProviderConnectionUseCase;
import com.h2traindata.application.usecase.HandleAuthorizationCallbackUseCase;
import com.h2traindata.application.usecase.StartAuthorizationUseCase;
import com.h2traindata.application.usecase.SyncAllProviderEventsUseCase;
import com.h2traindata.application.usecase.SyncProviderEventsUseCase;
import com.h2traindata.application.usecase.UpdateSyncPreferencesUseCase;
import com.h2traindata.domain.AthleteProfile;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.web.auth.AuthenticatedSession;
import com.h2traindata.web.auth.ProviderOAuthStateStore;
import com.h2traindata.web.mapper.SyncSettingsMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

class AuthControllerUnitTest {

    private final StartAuthorizationUseCase startAuthorizationUseCase = Mockito.mock(StartAuthorizationUseCase.class);
    private final GetProviderConnectionUseCase getProviderConnectionUseCase = Mockito.mock(GetProviderConnectionUseCase.class);
    private final HandleAuthorizationCallbackUseCase handleAuthorizationCallbackUseCase =
            Mockito.mock(HandleAuthorizationCallbackUseCase.class);
    private final SyncProviderEventsUseCase syncProviderEventsUseCase = Mockito.mock(SyncProviderEventsUseCase.class);
    private final SyncAllProviderEventsUseCase syncAllProviderEventsUseCase = Mockito.mock(SyncAllProviderEventsUseCase.class);
    private final UpdateSyncPreferencesUseCase updateSyncPreferencesUseCase = Mockito.mock(UpdateSyncPreferencesUseCase.class);
    private final ProviderRegistry providerRegistry = Mockito.mock(ProviderRegistry.class);
    private final SyncSettingsMapper syncSettingsMapper = Mockito.mock(SyncSettingsMapper.class);
    private final AuthenticatedSession authenticatedSession = new AuthenticatedSession();
    private final ProviderOAuthStateStore providerOAuthStateStore = new ProviderOAuthStateStore();

    private final AuthController controller = new AuthController(
            startAuthorizationUseCase,
            getProviderConnectionUseCase,
            handleAuthorizationCallbackUseCase,
            syncProviderEventsUseCase,
            syncAllProviderEventsUseCase,
            updateSyncPreferencesUseCase,
            providerRegistry,
            syncSettingsMapper,
            authenticatedSession,
            providerOAuthStateStore
    );

    @Test
    void callbackRedirectsEvenWhenInitialSyncHitsProviderRateLimit() {
        ProviderConnection connection = new ProviderConnection(
                "fitbit",
                new AthleteProfile("ABC123", "fitbit-runner"),
                "access-token",
                "refresh-token",
                Instant.now().plusSeconds(600)
        ).withUserId("internal-user-1");
        when(handleAuthorizationCallbackUseCase.execute("fitbit", "oauth-code", "internal-user-1")).thenReturn(connection);
        doThrow(new ProviderRateLimitException("fitbit", "fetch Fitbit activity logs", 60L, null))
                .when(syncAllProviderEventsUseCase)
                .execute("fitbit", "ABC123");

        MockHttpSession session = new MockHttpSession();
        String state = providerOAuthStateStore.createState(session, "internal-user-1");

        ResponseEntity<Void> response = controller.callback(
                "fitbit",
                "oauth-code",
                state,
                session
        );

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().contains("connectedProvider=fitbit"));
        assertTrue(response.getHeaders().getLocation().toString().contains("athleteId=ABC123"));
    }

    @Test
    void callbackRedirectsProviderAlreadyLinkedErrorsBackToPortal() {
        when(handleAuthorizationCallbackUseCase.execute("strava", "oauth-code", "internal-user-2"))
                .thenThrow(new ProviderConnectionAlreadyLinkedException("strava", "1143069702"));

        MockHttpSession session = new MockHttpSession();
        String state = providerOAuthStateStore.createState(session, "internal-user-2");

        ResponseEntity<Void> response = controller.callback(
                "strava",
                "oauth-code",
                state,
                session
        );

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        String location = response.getHeaders().getLocation().toString();
        assertTrue(location.contains("providerError=already_linked"));
        assertTrue(location.contains("provider=strava"));
        assertTrue(location.contains("athleteId=1143069702"));
    }
}
