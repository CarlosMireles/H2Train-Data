package com.h2traindata.web;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.port.out.ProviderCatalog;
import com.h2traindata.application.usecase.GetUserAccountUseCase;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.web.auth.AuthenticatedUserContext;
import com.h2traindata.web.mapper.SyncSettingsMapper;
import com.h2traindata.web.portal.PortalPageRenderer;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortalController {

    private final ProviderCatalog providerCatalog;
    private final GetUserAccountUseCase getUserAccountUseCase;
    private final ConnectionRepository connectionRepository;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final SyncSettingsMapper syncSettingsMapper;
    private final PortalPageRenderer portalPageRenderer;

    public PortalController(ProviderCatalog providerCatalog,
                            GetUserAccountUseCase getUserAccountUseCase,
                            ConnectionRepository connectionRepository,
                            AuthenticatedUserContext authenticatedUserContext,
                            SyncSettingsMapper syncSettingsMapper,
                            PortalPageRenderer portalPageRenderer) {
        this.providerCatalog = providerCatalog;
        this.getUserAccountUseCase = getUserAccountUseCase;
        this.connectionRepository = connectionRepository;
        this.authenticatedUserContext = authenticatedUserContext;
        this.syncSettingsMapper = syncSettingsMapper;
        this.portalPageRenderer = portalPageRenderer;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> home(HttpServletRequest request) {
        return authenticatedUserContext.currentUserId(request)
                .map(userId -> {
                    InternalUserAccount userAccount = getUserAccountUseCase.execute(userId);
                    String page = portalPageRenderer.render(
                            providerCatalog.registeredProviderIds(),
                            userAccount,
                            connectionRepository.findByUserId(userId).stream()
                                    .map(syncSettingsMapper::toResponse)
                                    .toList()
                    );
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(page);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/login"))
                        .build());
    }
}
