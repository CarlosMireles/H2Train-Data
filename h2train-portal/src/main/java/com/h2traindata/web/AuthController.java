package com.h2traindata.web;

import com.h2traindata.application.exception.AuthenticationRequiredException;
import com.h2traindata.application.exception.ForbiddenAccountAccessException;
import com.h2traindata.application.exception.ProviderRateLimitException;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.application.usecase.GetProviderConnectionUseCase;
import com.h2traindata.application.usecase.HandleAuthorizationCallbackUseCase;
import com.h2traindata.application.usecase.SyncProviderEventsUseCase;
import com.h2traindata.application.usecase.StartAuthorizationUseCase;
import com.h2traindata.application.usecase.SyncAllProviderEventsUseCase;
import com.h2traindata.application.usecase.UpdateSyncPreferencesUseCase;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.web.dto.SyncEventsResponse;
import com.h2traindata.web.dto.SyncSettingsRequest;
import com.h2traindata.web.dto.SyncSettingsResponse;
import com.h2traindata.web.auth.AuthenticatedSession;
import com.h2traindata.web.auth.ProviderOAuthStateStore;
import com.h2traindata.web.mapper.SyncSettingsMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final StartAuthorizationUseCase startAuthorizationUseCase;
    private final GetProviderConnectionUseCase getProviderConnectionUseCase;
    private final HandleAuthorizationCallbackUseCase handleAuthorizationCallbackUseCase;
    private final SyncProviderEventsUseCase syncProviderEventsUseCase;
    private final SyncAllProviderEventsUseCase syncAllProviderEventsUseCase;
    private final UpdateSyncPreferencesUseCase updateSyncPreferencesUseCase;
    private final ProviderRegistry providerRegistry;
    private final SyncSettingsMapper syncSettingsMapper;
    private final AuthenticatedSession authenticatedSession;
    private final ProviderOAuthStateStore providerOAuthStateStore;

    public AuthController(StartAuthorizationUseCase startAuthorizationUseCase,
                          GetProviderConnectionUseCase getProviderConnectionUseCase,
                          HandleAuthorizationCallbackUseCase handleAuthorizationCallbackUseCase,
                          SyncProviderEventsUseCase syncProviderEventsUseCase,
                          SyncAllProviderEventsUseCase syncAllProviderEventsUseCase,
                          UpdateSyncPreferencesUseCase updateSyncPreferencesUseCase,
                          ProviderRegistry providerRegistry,
                          SyncSettingsMapper syncSettingsMapper,
                          AuthenticatedSession authenticatedSession,
                          ProviderOAuthStateStore providerOAuthStateStore) {
        this.startAuthorizationUseCase = startAuthorizationUseCase;
        this.getProviderConnectionUseCase = getProviderConnectionUseCase;
        this.handleAuthorizationCallbackUseCase = handleAuthorizationCallbackUseCase;
        this.syncProviderEventsUseCase = syncProviderEventsUseCase;
        this.syncAllProviderEventsUseCase = syncAllProviderEventsUseCase;
        this.updateSyncPreferencesUseCase = updateSyncPreferencesUseCase;
        this.providerRegistry = providerRegistry;
        this.syncSettingsMapper = syncSettingsMapper;
        this.authenticatedSession = authenticatedSession;
        this.providerOAuthStateStore = providerOAuthStateStore;
    }

    @GetMapping("/{provider}/login")
    public ResponseEntity<Void> login(@PathVariable String provider,
                                      HttpServletRequest request) {
        String userId = authenticatedSession.currentUserId(request).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/login?error=session_required"))
                    .build();
        }

        String state = providerOAuthStateStore.createState(request.getSession(true), userId);
        URI authorizationUri = startAuthorizationUseCase.execute(provider, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizationUri.toString())
                .build();
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider,
                                         @RequestParam("code") String code,
                                         @RequestParam(value = "state", required = false) String state,
                                         HttpSession session) {
        String userId = providerOAuthStateStore.consumeUserId(session, state)
                .orElseThrow(AuthenticationRequiredException::new);
        ProviderConnection connection = handleAuthorizationCallbackUseCase.execute(provider, code, userId);
        session.setAttribute(AuthenticatedSession.USER_ID_ATTRIBUTE, connection.userId());
        try {
            syncAllProviderEventsUseCase.execute(provider, connection.athlete().id());
        } catch (ProviderRateLimitException exception) {
            log.warn("Initial sync deferred because provider={} athlete={} hit a rate limit during operation={} retryAfterSeconds={}",
                    provider,
                    connection.athlete().id(),
                    exception.operation(),
                    exception.retryAfterSeconds());
        }

        URI redirectUri = UriComponentsBuilder.fromPath("/")
                .queryParam("connectedProvider", provider)
                .queryParam("athleteId", connection.athlete().id())
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @GetMapping("/{provider}/athletes/{athleteId}/sync/{eventType}")
    public SyncEventsResponse syncEvents(@PathVariable String provider,
                                         @PathVariable String athleteId,
                                         @PathVariable EventType eventType,
                                         HttpServletRequest request) {
        ensureOwnedConnection(provider, athleteId, request);
        EventBatch batch = syncProviderEventsUseCase.execute(provider, athleteId, eventType, null);
        return new SyncEventsResponse(
                provider,
                athleteId,
                eventType.name(),
                batch.events().size(),
                "Events collected successfully"
        );
    }

    @GetMapping("/{provider}/athletes/{athleteId}/sync-settings")
    public SyncSettingsResponse getSyncSettings(@PathVariable String provider,
                                                @PathVariable String athleteId,
                                                HttpServletRequest request) {
        ProviderConnection connection = ensureOwnedConnection(provider, athleteId, request);
        return syncSettingsMapper.toResponse(connection);
    }

    @PutMapping(
            value = "/{provider}/athletes/{athleteId}/sync-settings",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SyncSettingsResponse updateSyncSettings(@PathVariable String provider,
                                                   @PathVariable String athleteId,
                                                   @RequestBody SyncSettingsRequest request,
                                                   HttpServletRequest httpRequest) {
        ProviderConnection existingConnection = ensureOwnedConnection(provider, athleteId, httpRequest);
        ProviderConnection updatedConnection = updateSyncPreferencesUseCase.execute(
                provider,
                athleteId,
                syncSettingsMapper.merge(existingConnection, request)
        );
        return syncSettingsMapper.toResponse(updatedConnection);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "providers", String.join(",", providerRegistry.registeredProviderIds())
        );
    }

    private ProviderConnection ensureOwnedConnection(String provider, String athleteId, HttpServletRequest request) {
        String userId = authenticatedSession.requireUserId(request);
        ProviderConnection connection = getProviderConnectionUseCase.execute(provider, athleteId);
        if (!userId.equals(connection.userId())) {
            throw new ForbiddenAccountAccessException();
        }
        return connection;
    }
}
