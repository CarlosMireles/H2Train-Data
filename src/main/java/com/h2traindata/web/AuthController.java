package com.h2traindata.web;

import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.application.usecase.HandleAuthorizationCallbackUseCase;
import com.h2traindata.application.usecase.StartAuthorizationUseCase;
import com.h2traindata.application.usecase.SyncProviderEventsUseCase;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.web.dto.ConnectAthleteResponse;
import com.h2traindata.web.dto.SyncEventsResponse;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final StartAuthorizationUseCase startAuthorizationUseCase;
    private final HandleAuthorizationCallbackUseCase handleAuthorizationCallbackUseCase;
    private final SyncProviderEventsUseCase syncProviderEventsUseCase;
    private final ProviderRegistry providerRegistry;

    public AuthController(StartAuthorizationUseCase startAuthorizationUseCase,
                          HandleAuthorizationCallbackUseCase handleAuthorizationCallbackUseCase,
                          SyncProviderEventsUseCase syncProviderEventsUseCase,
                          ProviderRegistry providerRegistry) {
        this.startAuthorizationUseCase = startAuthorizationUseCase;
        this.handleAuthorizationCallbackUseCase = handleAuthorizationCallbackUseCase;
        this.syncProviderEventsUseCase = syncProviderEventsUseCase;
        this.providerRegistry = providerRegistry;
    }

    @GetMapping("/{provider}/login")
    public ResponseEntity<Void> login(@PathVariable String provider) {
        URI authorizationUri = startAuthorizationUseCase.execute(provider);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authorizationUri.toString())
                .build();
    }

    @GetMapping("/{provider}/callback")
    public ConnectAthleteResponse callback(@PathVariable String provider, @RequestParam("code") String code) {
        ProviderConnection connection = handleAuthorizationCallbackUseCase.execute(provider, code);
        EventBatch batch = syncProviderEventsUseCase.execute(provider, connection.athlete().id(), EventType.ACTIVITY, null);

        return new ConnectAthleteResponse(
                provider,
                connection.athlete().id(),
                connection.athlete().username(),
                EventType.ACTIVITY.name(),
                batch.events().size(),
                "Athlete connected and initial activity sync sent to the event sink"
        );
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

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "providers", String.join(",", providerRegistry.registeredProviderIds())
        );
    }
}
