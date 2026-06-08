package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.EmailAlreadyInUseException;
import com.h2traindata.application.exception.EmailConfirmationMismatchException;
import com.h2traindata.application.exception.EmailUnchangedException;
import com.h2traindata.application.exception.InvalidCurrentPasswordException;
import com.h2traindata.application.exception.UserAccountNotFoundException;
import com.h2traindata.application.port.out.PasswordHasher;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountCredentialPolicy;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.domain.InternalUserAccount;
import org.springframework.stereotype.Service;

@Service
public class ChangeUserEmailUseCase {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHasher passwordHasher;
    private final AccountCredentialPolicy accountCredentialPolicy;
    private final AccountEventPublisher accountEventPublisher;

    public ChangeUserEmailUseCase(UserAccountRepository userAccountRepository,
                                  PasswordHasher passwordHasher,
                                  AccountCredentialPolicy accountCredentialPolicy,
                                  AccountEventPublisher accountEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHasher = passwordHasher;
        this.accountCredentialPolicy = accountCredentialPolicy;
        this.accountEventPublisher = accountEventPublisher;
    }

    public InternalUserAccount execute(String userId,
                                       String newEmail,
                                       String confirmNewEmail,
                                       String currentPassword) {
        InternalUserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserAccountNotFoundException(userId));
        String normalizedEmail = accountCredentialPolicy.normalizeEmail(newEmail);
        String normalizedConfirmation = accountCredentialPolicy.normalizeEmail(confirmNewEmail);
        if (!normalizedEmail.equals(normalizedConfirmation)) {
            throw new EmailConfirmationMismatchException();
        }
        String currentEmail = userAccount.email() == null
                ? ""
                : accountCredentialPolicy.normalizeEmail(userAccount.email());
        if (normalizedEmail.equals(currentEmail)) {
            throw new EmailUnchangedException();
        }
        if (!passwordHasher.matches(currentPassword, userAccount.passwordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        userAccountRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.id().equals(userAccount.id()))
                .ifPresent(existing -> {
                    throw new EmailAlreadyInUseException();
                });

        InternalUserAccount updated = userAccountRepository.save(userAccount.withEmail(normalizedEmail));
        accountEventPublisher.publishUserEmailChanged(updated);
        return updated;
    }
}
