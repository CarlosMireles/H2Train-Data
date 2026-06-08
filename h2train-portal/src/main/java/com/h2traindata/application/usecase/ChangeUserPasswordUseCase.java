package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.AccountManagedExternallyException;
import com.h2traindata.application.exception.InvalidCurrentPasswordException;
import com.h2traindata.application.exception.PasswordConfirmationMismatchException;
import com.h2traindata.application.exception.PasswordUnchangedException;
import com.h2traindata.application.exception.UserAccountNotFoundException;
import com.h2traindata.application.port.out.PasswordHasher;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountCredentialPolicy;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.domain.InternalUserAccount;
import org.springframework.stereotype.Service;

@Service
public class ChangeUserPasswordUseCase {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHasher passwordHasher;
    private final AccountCredentialPolicy accountCredentialPolicy;
    private final AccountEventPublisher accountEventPublisher;

    public ChangeUserPasswordUseCase(UserAccountRepository userAccountRepository,
                                     PasswordHasher passwordHasher,
                                     AccountCredentialPolicy accountCredentialPolicy,
                                     AccountEventPublisher accountEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHasher = passwordHasher;
        this.accountCredentialPolicy = accountCredentialPolicy;
        this.accountEventPublisher = accountEventPublisher;
    }

    public InternalUserAccount execute(String userId,
                                       String currentPassword,
                                       String newPassword,
                                       String confirmNewPassword) {
        InternalUserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserAccountNotFoundException(userId));
        if (!userAccount.hasLocalCredentials()) {
            throw new AccountManagedExternallyException();
        }
        if (!passwordHasher.matches(currentPassword, userAccount.passwordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        accountCredentialPolicy.validatePassword(newPassword);
        if (!newPassword.equals(confirmNewPassword)) {
            throw new PasswordConfirmationMismatchException();
        }
        if (passwordHasher.matches(newPassword, userAccount.passwordHash())) {
            throw new PasswordUnchangedException();
        }

        InternalUserAccount updated = userAccountRepository.save(userAccount.withPasswordHash(passwordHasher.hash(newPassword)));
        accountEventPublisher.publishUserPasswordChanged(updated);
        // TODO: Invalidate other active sessions when the portal has a centralized session/token store.
        return updated;
    }
}
