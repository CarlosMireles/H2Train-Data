package com.h2traindata.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.h2traindata.application.exception.AccountManagedExternallyException;
import com.h2traindata.application.exception.ExpiredPasswordResetTokenException;
import com.h2traindata.application.exception.InvalidPasswordResetTokenException;
import com.h2traindata.application.exception.PasswordConfirmationMismatchException;
import com.h2traindata.application.exception.PasswordUnchangedException;
import com.h2traindata.application.port.out.PasswordResetMessageSender;
import com.h2traindata.application.service.AccountCredentialPolicy;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.application.service.PasswordHashService;
import com.h2traindata.application.service.PasswordResetTokenService;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.PasswordResetToken;
import com.h2traindata.infrastructure.persistence.InMemoryPasswordResetTokenRepository;
import com.h2traindata.infrastructure.persistence.InMemoryUserAccountRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PasswordResetUseCaseTest {

    private static final String USER_ID = "user-1";
    private static final Instant NOW = Instant.parse("2026-06-08T10:00:00Z");

    private InMemoryUserAccountRepository userAccountRepository;
    private InMemoryPasswordResetTokenRepository passwordResetTokenRepository;
    private PasswordHashService passwordHashService;
    private PasswordResetTokenService passwordResetTokenService;
    private CapturingPasswordResetMessageSender messageSender;
    private RequestPasswordResetUseCase requestUseCase;
    private ResetPasswordWithTokenUseCase resetUseCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        userAccountRepository = new InMemoryUserAccountRepository();
        passwordResetTokenRepository = new InMemoryPasswordResetTokenRepository();
        passwordHashService = new PasswordHashService();
        passwordResetTokenService = new PasswordResetTokenService();
        messageSender = new CapturingPasswordResetMessageSender();
        requestUseCase = new RequestPasswordResetUseCase(
                userAccountRepository,
                passwordResetTokenRepository,
                messageSender,
                passwordResetTokenService,
                clock
        );
        resetUseCase = new ResetPasswordWithTokenUseCase(
                userAccountRepository,
                passwordResetTokenRepository,
                passwordHashService,
                new AccountCredentialPolicy(),
                Mockito.mock(AccountEventPublisher.class),
                passwordResetTokenService,
                clock
        );
        userAccountRepository.save(new InternalUserAccount(
                USER_ID,
                "runner@example.com",
                "runner",
                passwordHashService.hash("current-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));
    }

    @Test
    void requestsResetEmailForAccountEmail() {
        requestUseCase.execute(USER_ID, "http://localhost:8080/account/password/reset");

        assertEquals("runner@example.com", messageSender.email);
        assertEquals("runner", messageSender.username);
        assertTrue(messageSender.resetUri.toString().startsWith("http://localhost:8080/account/password/reset?token="));
        assertEquals(NOW.plusSeconds(1800), messageSender.expiresAt);
    }

    @Test
    void publicRequestSendsResetForLocalAccountEmail() {
        requestUseCase.executeForEmail("runner@example.com", "http://localhost:8080/account/password/reset");

        assertEquals("runner@example.com", messageSender.email);
        assertEquals("runner", messageSender.username);
        assertTrue(messageSender.resetUri.toString().startsWith("http://localhost:8080/account/password/reset?token="));
    }

    @Test
    void publicRequestDoesNotRevealMissingOrExternalAccounts() {
        requestUseCase.executeForEmail("missing@example.com", "http://localhost:8080/account/password/reset");
        assertEquals(null, messageSender.email);

        userAccountRepository.save(new InternalUserAccount(
                "google-user",
                "google@example.com",
                "google-runner",
                null,
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        requestUseCase.executeForEmail("google@example.com", "http://localhost:8080/account/password/reset");
        assertEquals(null, messageSender.email);
    }

    @Test
    void resetsPasswordWithTokenFromRecoveryLink() {
        String previousHash = userAccountRepository.findById(USER_ID).orElseThrow().passwordHash();
        requestUseCase.execute(USER_ID, "http://localhost:8080/account/password/reset");

        resetUseCase.execute(tokenFromLink(), "next-password", "next-password");

        String updatedHash = userAccountRepository.findById(USER_ID).orElseThrow().passwordHash();
        assertNotEquals(previousHash, updatedHash);
        assertTrue(passwordHashService.matches("next-password", updatedHash));
        assertThrows(InvalidPasswordResetTokenException.class, () -> resetUseCase.execute(
                tokenFromLink(),
                "another-password",
                "another-password"
        ));
    }

    @Test
    void rejectsInvalidOrExpiredToken() {
        assertThrows(InvalidPasswordResetTokenException.class, () -> resetUseCase.execute(
                "unknown-token",
                "next-password",
                "next-password"
        ));

        String expiredToken = "expired-token";
        passwordResetTokenRepository.save(new PasswordResetToken(
                passwordResetTokenService.hashToken(expiredToken),
                USER_ID,
                "runner@example.com",
                NOW.minusSeconds(1),
                NOW.minusSeconds(3600),
                null
        ));

        assertThrows(ExpiredPasswordResetTokenException.class, () -> resetUseCase.execute(
                expiredToken,
                "next-password",
                "next-password"
        ));
    }

    @Test
    void rejectsInvalidPasswordResetInput() {
        requestUseCase.execute(USER_ID, "http://localhost:8080/account/password/reset");

        assertThrows(PasswordConfirmationMismatchException.class, () -> resetUseCase.execute(
                tokenFromLink(),
                "next-password",
                "different-password"
        ));
        assertThrows(PasswordUnchangedException.class, () -> resetUseCase.execute(
                tokenFromLink(),
                "current-password",
                "current-password"
        ));
    }

    @Test
    void rejectsResetRequestForGoogleOnlyAccounts() {
        userAccountRepository.save(new InternalUserAccount(
                "google-user",
                "google@example.com",
                "google-runner",
                null,
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        assertThrows(AccountManagedExternallyException.class, () -> requestUseCase.execute(
                "google-user",
                "http://localhost:8080/account/password/reset"
        ));
    }

    @Test
    void rejectsResetTokenForGoogleOnlyAccounts() {
        userAccountRepository.save(new InternalUserAccount(
                "google-user",
                "google@example.com",
                "google-runner",
                null,
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));
        String token = "legacy-token";
        passwordResetTokenRepository.save(new PasswordResetToken(
                passwordResetTokenService.hashToken(token),
                "google-user",
                "google@example.com",
                NOW.plusSeconds(1800),
                NOW,
                null
        ));

        assertThrows(AccountManagedExternallyException.class, () -> resetUseCase.execute(
                token,
                "next-password",
                "next-password"
        ));
    }

    private String tokenFromLink() {
        return messageSender.resetUri.getQuery().substring("token=".length());
    }

    private static final class CapturingPasswordResetMessageSender implements PasswordResetMessageSender {

        private String email;
        private String username;
        private URI resetUri;
        private Instant expiresAt;

        @Override
        public void sendPasswordReset(String email, String username, URI resetUri, Instant expiresAt) {
            this.email = email;
            this.username = username;
            this.resetUri = resetUri;
            this.expiresAt = expiresAt;
        }
    }
}
