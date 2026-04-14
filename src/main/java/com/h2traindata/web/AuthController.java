package com.h2traindata.web;

import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.application.usecase.GetProviderConnectionUseCase;
import com.h2traindata.application.usecase.HandleAuthorizationCallbackUseCase;
import com.h2traindata.application.usecase.StartAuthorizationUseCase;
import com.h2traindata.application.usecase.SyncProviderEventsUseCase;
import com.h2traindata.application.usecase.UpdateSyncPreferencesUseCase;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.web.dto.SyncEventsResponse;
import com.h2traindata.web.dto.SyncSettingsRequest;
import com.h2traindata.web.dto.SyncSettingsResponse;
import com.h2traindata.web.mapper.SyncSettingsMapper;
import java.net.URI;
import java.util.Map;
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

    private final StartAuthorizationUseCase startAuthorizationUseCase;
    private final GetProviderConnectionUseCase getProviderConnectionUseCase;
    private final HandleAuthorizationCallbackUseCase handleAuthorizationCallbackUseCase;
    private final SyncProviderEventsUseCase syncProviderEventsUseCase;
    private final UpdateSyncPreferencesUseCase updateSyncPreferencesUseCase;
    private final ProviderRegistry providerRegistry;
    private final SyncSettingsMapper syncSettingsMapper;

    public AuthController(StartAuthorizationUseCase startAuthorizationUseCase,
                          GetProviderConnectionUseCase getProviderConnectionUseCase,
                          HandleAuthorizationCallbackUseCase handleAuthorizationCallbackUseCase,
                          SyncProviderEventsUseCase syncProviderEventsUseCase,
                          UpdateSyncPreferencesUseCase updateSyncPreferencesUseCase,
                          ProviderRegistry providerRegistry,
                          SyncSettingsMapper syncSettingsMapper) {
        this.startAuthorizationUseCase = startAuthorizationUseCase;
        this.getProviderConnectionUseCase = getProviderConnectionUseCase;
        this.handleAuthorizationCallbackUseCase = handleAuthorizationCallbackUseCase;
        this.syncProviderEventsUseCase = syncProviderEventsUseCase;
        this.updateSyncPreferencesUseCase = updateSyncPreferencesUseCase;
        this.providerRegistry = providerRegistry;
        this.syncSettingsMapper = syncSettingsMapper;
    }

    @GetMapping("/{provider}/login")
    public ResponseEntity<Void> login(@PathVariable String provider) {
        URI authorizationUri = startAuthorizationUseCase.execute(provider);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizationUri.toString())
                .build();
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider, @RequestParam("code") String code) {
        ProviderConnection connection = handleAuthorizationCallbackUseCase.execute(provider, code);
        syncProviderEventsUseCase.execute(provider, connection.athlete().id(), EventType.ACTIVITY, null);

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
                                         @PathVariable EventType eventType) {
        EventBatch batch = syncProviderEventsUseCase.execute(provider, athleteId, eventType, null);
        return new SyncEventsResponse(
                provider,
                athleteId,
                eventType.name(),
                batch.events().size(),
                "Events collected and sent to the configured event sink"
        );
    }

    @GetMapping("/{provider}/athletes/{athleteId}/sync-settings")
    public SyncSettingsResponse getSyncSettings(@PathVariable String provider,
                                                @PathVariable String athleteId) {
        ProviderConnection connection = getProviderConnectionUseCase.execute(provider, athleteId);
        return syncSettingsMapper.toResponse(connection);
    }

    @PutMapping(
            value = "/{provider}/athletes/{athleteId}/sync-settings",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SyncSettingsResponse updateSyncSettings(@PathVariable String provider,
                                                   @PathVariable String athleteId,
                                                   @RequestBody SyncSettingsRequest request) {
        ProviderConnection existingConnection = getProviderConnectionUseCase.execute(provider, athleteId);
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
}
