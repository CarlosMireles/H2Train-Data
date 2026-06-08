package com.h2traindata.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.h2traindata.application.exception.AccountManagedExternallyException;
import com.h2traindata.application.exception.EmailAlreadyInUseException;
import com.h2traindata.application.exception.EmailConfirmationMismatchException;
import com.h2traindata.application.exception.InvalidCurrentPasswordException;
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

class ChangeUserEmailUseCaseTest {

    private static final String USER_ID = "user-1";

    private InMemoryUserAccountRepository userAccountRepository;
    private PasswordHashService passwordHashService;
    private ChangeUserEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        userAccountRepository = new InMemoryUserAccountRepository();
        passwordHashService = new PasswordHashService();
        useCase = new ChangeUserEmailUseCase(
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
    void changesEmailWhenCurrentPasswordAndConfirmationAreValid() {
        InternalUserAccount updated = useCase.execute(
                USER_ID,
                "new-runner@example.com",
                "new-runner@example.com",
                "current-password"
        );

        assertEquals("new-runner@example.com", updated.email());
        assertEquals("new-runner@example.com", userAccountRepository.findById(USER_ID).orElseThrow().email());
        assertTrue(userAccountRepository.findByEmail("runner@example.com").isEmpty());
    }

    @Test
    void rejectsEmailAlreadyRegisteredByAnotherUser() {
        userAccountRepository.save(new InternalUserAccount(
                "user-2",
                "existing@example.com",
                "existing",
                passwordHashService.hash("other-password"),
                Set.of(),
                Instant.parse("2026-04-01T10:00:00Z")
        ));

        assertThrows(EmailAlreadyInUseException.class, () -> useCase.execute(
                USER_ID,
                "existing@example.com",
                "existing@example.com",
                "current-password"
        ));
    }

    @Test
    void rejectsInvalidCurrentPassword() {
        assertThrows(InvalidCurrentPasswordException.class, () -> useCase.execute(
                USER_ID,
                "new-runner@example.com",
                "new-runner@example.com",
                "wrong-password"
        ));
    }

    @Test
    void rejectsEmailConfirmationMismatch() {
        assertThrows(EmailConfirmationMismatchException.class, () -> useCase.execute(
                USER_ID,
                "new-runner@example.com",
                "other@example.com",
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
                "new-google@example.com",
                "new-google@example.com",
                "ignored-password"
        ));
    }
}
