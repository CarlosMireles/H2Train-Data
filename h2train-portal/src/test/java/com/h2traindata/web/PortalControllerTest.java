package com.h2traindata.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.web.auth.AuthenticatedSession;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "app.strava.client-id=12345",
        "app.strava.client-secret=test-secret",
        "app.strava.redirect-uri=http://localhost:8080/auth/strava/callback",
        "app.fitbit.enabled=true",
        "app.fitbit.client-id=fitbit-client",
        "app.fitbit.client-secret=fitbit-secret",
        "app.fitbit.redirect-uri=http://localhost:8080/auth/fitbit/callback"
})
@AutoConfigureMockMvc
class PortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void homeRedirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/login"));
    }

    @Test
    void homeRendersPortalForAuthenticatedUser() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "internal-user-portal",
                "portal@example.com",
                "portal-user",
                "hash",
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(get("/")
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "internal-user-portal"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("H2Train Data")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("portal@example.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("portal-user")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fitbit")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Strava")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/h2train-logo.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Automatic sync")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Every 5 hours")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Every 7 days")));
    }

    @Test
    void homeRendersProviderConnectionErrorsAsFriendlyAlert() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "internal-user-provider-error",
                "provider-error@example.com",
                "provider-error-user",
                "hash",
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(get("/")
                        .queryParam("providerError", "already_linked")
                        .queryParam("provider", "strava")
                        .queryParam("athleteId", "1143069702")
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "internal-user-provider-error"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Strava account already linked")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Provider account 'strava/1143069702'")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("use a private window")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/auth/strava/login")));
    }
}
