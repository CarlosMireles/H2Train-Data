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
import com.h2traindata.web.auth.RememberMeService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "app.strava.client-id=12345",
        "app.strava.client-secret=test-secret",
        "app.strava.redirect-uri=http://localhost:8080/auth/strava/callback",
        "app.fitbit.enabled=true",
        "app.fitbit.client-id=fitbit-client",
        "app.fitbit.client-secret=fitbit-secret",
        "app.fitbit.redirect-uri=http://localhost:8080/auth/fitbit/callback",
        "app.auth.google.enabled=true",
        "app.auth.google.client-id=google-client",
        "app.auth.google.client-secret=google-secret",
        "app.auth.google.redirect-uri=http://localhost:8080/auth/google/callback"
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-layout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-brand-panel")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-login-card")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome back")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-action=\"toggle-password\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"rememberMe\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/forgot-password\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("google-logo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("All systems operational")));
    }

    @Test
    void registerPageUsesPortalStyles() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/h2train-logo.png")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/portal.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-layout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-brand-panel")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-login-card")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"confirmPassword\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirm password")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Create your account")));
    }

    @Test
    void registerCreatesInternalAccountAndSession() throws Exception {
        mockMvc.perform(post("/account/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "new-runner")
                        .param("email", "new-runner@example.com")
                        .param("password", "correct-password")
                        .param("confirmPassword", "correct-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/"))
                .andExpect(request().sessionAttribute(AuthenticatedSession.USER_ID_ATTRIBUTE, org.hamcrest.Matchers.notNullValue()));

        InternalUserAccount userAccount = userAccountRepository.findByEmail("new-runner@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("new-runner", userAccount.username());
        org.junit.jupiter.api.Assertions.assertNotEquals("correct-password", userAccount.passwordHash());
    }

    @Test
    void registerRejectsMismatchedPasswordConfirmation() throws Exception {
        mockMvc.perform(post("/account/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "mismatch-runner")
                        .param("email", "mismatch@example.com")
                        .param("password", "correct-password")
                        .param("confirmPassword", "different-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/register?error=password_mismatch"));

        org.junit.jupiter.api.Assertions.assertTrue(userAccountRepository.findByEmail("mismatch@example.com").isEmpty());
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
    void loginWithRememberMeStoresPersistentCookieAndRestoresSession() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "remember-user",
                "remember@example.com",
                "remember-runner",
                passwordHashService.hash("correct-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        MvcResult loginResult = mockMvc.perform(post("/account/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("login", "remember-runner")
                        .param("password", "correct-password")
                        .param("rememberMe", "true"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/"))
                .andReturn();

        Cookie rememberCookie = loginResult.getResponse().getCookie(RememberMeService.COOKIE_NAME);
        org.junit.jupiter.api.Assertions.assertNotNull(rememberCookie);
        org.junit.jupiter.api.Assertions.assertTrue(rememberCookie.isHttpOnly());
        org.junit.jupiter.api.Assertions.assertTrue(rememberCookie.getMaxAge() > 0);

        mockMvc.perform(get("/")
                        .cookie(rememberCookie))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute(AuthenticatedSession.USER_ID_ATTRIBUTE, "remember-user"));

        mockMvc.perform(post("/account/logout")
                        .cookie(rememberCookie)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "remember-user"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/login"));

        mockMvc.perform(get("/")
                        .cookie(rememberCookie))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/login"));
    }

    @Test
    void forgotPasswordPageUsesPortalStyles() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Recover password")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-layout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-brand-panel")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"email\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/account/password/forgot\"")));
    }

    @Test
    void publicForgotPasswordRequestUsesGenericResponse() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "forgot-user",
                "forgot@example.com",
                "forgot-runner",
                passwordHashService.hash("current-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(post("/account/password/forgot")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "forgot@example.com"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/forgot-password?status=requested"));

        mockMvc.perform(post("/account/password/forgot")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "missing@example.com"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/forgot-password?status=requested"));
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

    @Test
    void changeEmailUpdatesAuthenticatedInternalAccount() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "email-change-user",
                "email-change@example.com",
                "email-change-runner",
                passwordHashService.hash("current-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(post("/account/email")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "email-change-user")
                        .param("newEmail", "updated-email@example.com")
                        .param("confirmNewEmail", "updated-email@example.com")
                        .param("currentPassword", "current-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?accountStatus=email_changed"));

        org.junit.jupiter.api.Assertions.assertEquals(
                "updated-email@example.com",
                userAccountRepository.findById("email-change-user").orElseThrow().email()
        );
    }

    @Test
    void changeEmailRejectsInvalidCurrentPassword() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "email-change-invalid-password-user",
                "email-change-invalid@example.com",
                "email-change-invalid-runner",
                passwordHashService.hash("current-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(post("/account/email")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "email-change-invalid-password-user")
                        .param("newEmail", "updated-invalid@example.com")
                        .param("confirmNewEmail", "updated-invalid@example.com")
                        .param("currentPassword", "wrong-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?accountError=invalid_current_password"));
    }

    @Test
    void changePasswordUpdatesAuthenticatedInternalAccount() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "password-change-user",
                "password-change@example.com",
                "password-change-runner",
                passwordHashService.hash("current-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(post("/account/password")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "password-change-user")
                        .param("currentPassword", "current-password")
                        .param("newPassword", "next-password")
                        .param("confirmNewPassword", "next-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?accountStatus=password_changed"));

        String updatedHash = userAccountRepository.findById("password-change-user").orElseThrow().passwordHash();
        org.junit.jupiter.api.Assertions.assertTrue(passwordHashService.matches("next-password", updatedHash));
    }

    @Test
    void changePasswordRejectsConfirmationMismatch() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "password-change-mismatch-user",
                "password-change-mismatch@example.com",
                "password-change-mismatch-runner",
                passwordHashService.hash("current-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(post("/account/password")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "password-change-mismatch-user")
                        .param("currentPassword", "current-password")
                        .param("newPassword", "next-password")
                        .param("confirmNewPassword", "different-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?accountError=password_mismatch"));
    }

    @Test
    void requestPasswordResetRequiresAuthenticatedAccountAndUsesStoredEmail() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "password-reset-request-user",
                "password-reset-request@example.com",
                "password-reset-request-runner",
                passwordHashService.hash("current-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(post("/account/password/reset/request")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "password-reset-request-user"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?accountStatus=password_reset_requested"));
    }

    @Test
    void resetPasswordPageRendersTokenForm() throws Exception {
        mockMvc.perform(get("/account/password/reset")
                        .param("token", "reset-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reset password")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-layout")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth-brand-panel")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"token\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"reset-token\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/account/password/reset\"")));
    }

    @Test
    void accountCredentialActionsRejectGoogleOnlyAccounts() throws Exception {
        userAccountRepository.save(new InternalUserAccount(
                "google-only-account-actions-user",
                "google-only-actions@example.com",
                "google-only-actions-runner",
                null,
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        mockMvc.perform(post("/account/email")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "google-only-account-actions-user")
                        .param("newEmail", "updated-google-only@example.com")
                        .param("confirmNewEmail", "updated-google-only@example.com")
                        .param("currentPassword", "ignored-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?accountError=external_account_managed"));

        mockMvc.perform(post("/account/password")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "google-only-account-actions-user")
                        .param("currentPassword", "ignored-password")
                        .param("newPassword", "next-password")
                        .param("confirmNewPassword", "next-password"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?accountError=external_account_managed"));

        mockMvc.perform(post("/account/password/reset/request")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .sessionAttr(AuthenticatedSession.USER_ID_ATTRIBUTE, "google-only-account-actions-user"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?accountError=external_account_managed"));
    }
}
