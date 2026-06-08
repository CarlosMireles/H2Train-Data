package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.DuplicateUserAccountException;
import com.h2traindata.application.port.out.PasswordHasher;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountCredentialPolicy;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.domain.InternalUserAccount;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RegisterUserAccountUseCase {

    private final UserAccountRepository userAccountRepository;
    private final PasswordHasher passwordHasher;
    private final AccountCredentialPolicy accountCredentialPolicy;
    private final AccountEventPublisher accountEventPublisher;

    public RegisterUserAccountUseCase(UserAccountRepository userAccountRepository,
                                      PasswordHasher passwordHasher,
                                      AccountCredentialPolicy accountCredentialPolicy,
                                      AccountEventPublisher accountEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHasher = passwordHasher;
        this.accountCredentialPolicy = accountCredentialPolicy;
        this.accountEventPublisher = accountEventPublisher;
    }

    public InternalUserAccount execute(String username, String email, String password) {
        String normalizedUsername = accountCredentialPolicy.required("username", username);
        String normalizedEmail = accountCredentialPolicy.normalizeEmail(email);
        accountCredentialPolicy.validatePassword(password);

        userAccountRepository.findByUsername(normalizedUsername)
                .ifPresent(existing -> {
                    throw new DuplicateUserAccountException("username");
                });
        userAccountRepository.findByEmail(normalizedEmail)
                .ifPresent(existing -> {
                    throw new DuplicateUserAccountException("email");
                });

        InternalUserAccount userAccount = userAccountRepository.save(new InternalUserAccount(
                UUID.randomUUID().toString(),
                normalizedEmail,
                normalizedUsername,
                passwordHasher.hash(password),
                Set.of(),
                Instant.now()
        ));
        accountEventPublisher.publishUserRegistered(userAccount, "password");
        return userAccount;
    }
}
