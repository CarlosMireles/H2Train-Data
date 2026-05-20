package com.h2traindata.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.PasswordHashService;
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
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordHashService passwordHashService;

    @Test
    void loginPageUsesPortalStyles() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/h2train-logo.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-hero")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account Portal")));
    }

    @Test
    void registerPageUsesPortalStyles() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/h2train-logo.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-hero")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account Portal")));
    }

    @Test
    void registerCreatesInternalAccountAndSession() throws Exception {
        mockMvc.perform(post("/account/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "new-runner")
                        .param("email", "new-runner@example.com")
                        .param("password", "correct-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/"))
                .andExpect(request().sessionAttribute(AuthenticatedSession.USER_ID_ATTRIBUTE, org.hamcrest.Matchers.notNullValue()));

        InternalUserAccount userAccount = userAccountRepository.findByEmail("new-runner@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("new-runner", userAccount.username());
        org.junit.jupiter.api.Assertions.assertNotEquals("correct-password", userAccount.passwordHash());
    }

    @Test
    void loginAuthenticatesWithUsernameOrEmail() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "login-user",
                "login@example.com",
                "login-runner",
                passwordHashService.hash("correct-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(post("/account/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("login", "login-runner")
                        .param("password", "correct-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/"))
                .andExpect(request().sessionAttribute(AuthenticatedSession.USER_ID_ATTRIBUTE, "login-user"));
    }

    @Test
    void accountMeReturnsAuthenticatedAccount() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "me-user",
                "me@example.com",
                "me-runner",
                "hash",
                Set.of("strava"),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(get("/account/me")
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "me-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("me-user"))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.username").value("me-runner"))
                .andExpect(jsonPath("$.providers[0]").value("strava"));
    }
}
