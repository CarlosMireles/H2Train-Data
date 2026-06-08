package com.h2traindata.application.usecase;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.h2traindata.application.exception.AccountManagedExternallyException;
import com.h2traindata.application.exception.InvalidCurrentPasswordException;
import com.h2traindata.application.exception.PasswordConfirmationMismatchException;
import com.h2traindata.application.exception.PasswordUnchangedException;
import com.h2traindata.application.service.AccountCredentialPolicy;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.application.service.PasswordHashService;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.infrastructure.persistence.InMemoryUserAccountRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChangeUserPasswordUseCaseTest {

    private static final String USER_ID = "user-1";

    private InMemoryUserAccountRepository userAccountRepository;
    private PasswordHashService passwordHashService;
    private ChangeUserPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        userAccountRepository = new InMemoryUserAccountRepository();
        passwordHashService = new PasswordHashService();
        useCase = new ChangeUserPasswordUseCase(
                userAccountRepository,
                passwordHashService,
                new AccountCredentialPolicy(),
                Mockito.mock(AccountEventPublisher.class)
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
    void changesPasswordUsingHasher() {
        String previousHash = userAccountRepository.findById(USER_ID).orElseThrow().passwordHash();

        useCase.execute(USER_ID, "current-password", "next-password", "next-password");

        String updatedHash = userAccountRepository.findById(USER_ID).orElseThrow().passwordHash();
        assertNotEquals(previousHash, updatedHash);
        assertTrue(passwordHashService.matches("next-password", updatedHash));
    }

    @Test
    void rejectsInvalidCurrentPassword() {
        assertThrows(InvalidCurrentPasswordException.class, () -> useCase.execute(
                USER_ID,
                "wrong-password",
                "next-password",
                "next-password"
        ));
    }

    @Test
    void rejectsPasswordConfirmationMismatch() {
        assertThrows(PasswordConfirmationMismatchException.class, () -> useCase.execute(
                USER_ID,
                "current-password",
                "next-password",
                "different-password"
        ));
    }

    @Test
    void rejectsNewPasswordEqualToCurrentPassword() {
        assertThrows(PasswordUnchangedException.class, () -> useCase.execute(
                USER_ID,
                "current-password",
                "current-password",
                "current-password"
        ));
    }

    @Test
    void rejectsGoogleOnlyAccountsWithoutLocalCredentials() {
        userAccountRepository.save(new InternalUserAccount(
                "google-user",
                "google@example.com",
                "google-runner",
                null,
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        assertThrows(AccountManagedExternallyException.class, () -> useCase.execute(
                "google-user",
                "ignored-password",
                "next-password",
                "next-password"
        ));
    }
}
