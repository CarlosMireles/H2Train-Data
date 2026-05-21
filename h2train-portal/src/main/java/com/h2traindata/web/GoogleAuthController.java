package com.h2traindata.web;

import com.h2traindata.application.usecase.AuthenticateGoogleUserUseCase;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.web.auth.AuthenticatedUserContext;
import com.h2traindata.web.auth.OAuthStateStore;
import com.h2traindata.web.identity.ExternalIdentityProfile;
import com.h2traindata.web.identity.ExternalIdentityProvider;
import com.h2traindata.web.identity.ExternalIdentityProviderCatalog;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleAuthController {

    private static final String GOOGLE_PROVIDER_ID = "google";

    private final ExternalIdentityProvider identityProvider;
    private final AuthenticateGoogleUserUseCase authenticateGoogleUserUseCase;
    private final AuthenticatedUserContext authenticatedUserContext;
    private final OAuthStateStore oAuthStateStore;

    public GoogleAuthController(ExternalIdentityProviderCatalog identityProviderCatalog,
                                AuthenticateGoogleUserUseCase authenticateGoogleUserUseCase,
                                AuthenticatedUserContext authenticatedUserContext,
                                OAuthStateStore oAuthStateStore) {
        this.identityProvider = identityProviderCatalog.provider(GOOGLE_PROVIDER_ID);
        this.authenticateGoogleUserUseCase = authenticateGoogleUserUseCase;
        this.authenticatedUserContext = authenticatedUserContext;
        this.oAuthStateStore = oAuthStateStore;
    }

    @GetMapping("/auth/google/login")
    public ResponseEntity<Void> login(HttpSession session) {
        if (!identityProvider.isConfigured()) {
            return redirect("/login?error=google_unavailable");
        }

        String state = oAuthStateStore.createState(session);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(identityProvider.authorizationUri(state))
                .build();
    }

    @GetMapping("/auth/google/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code,
                                         @RequestParam("state") String state,
                                         HttpSession session) {
        if (!oAuthStateStore.consumeState(session, state)) {
            return redirect("/login?error=google_state");
        }

        try {
            ExternalIdentityProfile profile = identityProvider.fetchProfile(code);
            InternalUserAccount userAccount = authenticateGoogleUserUseCase.execute(
                    profile.email(),
                    profile.displayName()
            );
            authenticatedUserContext.login(session, userAccount);
            return redirect("/");
        } catch (RuntimeException exception) {
            return redirect("/login?error=google_failed");
        }
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(location))
                .build();
    }
}
