package com.h2traindata.web;

import com.h2traindata.application.usecase.AuthenticateGoogleUserUseCase;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.web.auth.AuthenticatedSession;
import com.h2traindata.web.google.GoogleAuthProperties;
import com.h2traindata.web.google.GoogleOAuthClient;
import com.h2traindata.web.google.GoogleUserProfile;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleAuthController {

    private static final String GOOGLE_STATE_ATTRIBUTE = "h2train.googleOAuthState";

    private final GoogleAuthProperties googleAuthProperties;
    private final GoogleOAuthClient googleOAuthClient;
    private final AuthenticateGoogleUserUseCase authenticateGoogleUserUseCase;
    private final AuthenticatedSession authenticatedSession;
    private final SecureRandom secureRandom = new SecureRandom();

    public GoogleAuthController(GoogleAuthProperties googleAuthProperties,
                                GoogleOAuthClient googleOAuthClient,
                                AuthenticateGoogleUserUseCase authenticateGoogleUserUseCase,
                                AuthenticatedSession authenticatedSession) {
        this.googleAuthProperties = googleAuthProperties;
        this.googleOAuthClient = googleOAuthClient;
        this.authenticateGoogleUserUseCase = authenticateGoogleUserUseCase;
        this.authenticatedSession = authenticatedSession;
    }

    @GetMapping("/auth/google/login")
    public ResponseEntity<Void> login(HttpSession session) {
        if (!googleAuthProperties.isConfigured()) {
            return redirect("/login?error=google_unavailable");
        }

        String state = randomState();
        session.setAttribute(GOOGLE_STATE_ATTRIBUTE, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(googleOAuthClient.authorizationUri(state))
                .build();
    }

    @GetMapping("/auth/google/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code,
                                         @RequestParam("state") String state,
                                         HttpSession session) {
        Object expectedState = session.getAttribute(GOOGLE_STATE_ATTRIBUTE);
        session.removeAttribute(GOOGLE_STATE_ATTRIBUTE);
        if (!(expectedState instanceof String value) || !value.equals(state)) {
            return redirect("/login?error=google_state");
        }

        try {
            GoogleUserProfile profile = googleOAuthClient.fetchProfile(code);
            InternalUserAccount userAccount = authenticateGoogleUserUseCase.execute(
                    profile.email(),
                    profile.displayName()
            );
            authenticatedSession.login(session, userAccount);
            return redirect("/");
        } catch (RuntimeException exception) {
            return redirect("/login?error=google_failed");
        }
    }

    private String randomState() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(URI.create(location))
                .build();
    }
}
