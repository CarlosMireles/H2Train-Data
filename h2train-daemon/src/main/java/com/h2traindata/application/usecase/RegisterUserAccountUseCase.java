package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.DuplicateUserAccountException;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.application.service.PasswordHashService;
import com.h2traindata.domain.InternalUserAccount;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegisterUserAccountUseCase {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService;
    private final AccountEventPublisher accountEventPublisher;

    public RegisterUserAccountUseCase(UserAccountRepository userAccountRepository,
                                      PasswordHashService passwordHashService,
                                      AccountEventPublisher accountEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.passwordHashService = passwordHashService;
        this.accountEventPublisher = accountEventPublisher;
    }

    public InternalUserAccount execute(String username, String email, String password) {
        String normalizedUsername = required("username", username);
        String normalizedEmail = normalizeEmail(required("email", email));
        validatePassword(password);

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
                passwordHashService.hash(password),
                Set.of(),
                Instant.now()
        ));
        accountEventPublisher.publishUserRegistered(userAccount, "password");
        return userAccount;
    }

    private String required(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        if (!email.contains("@")) {
            throw new IllegalArgumentException("email must be valid");
        }
        return email.toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("password must have at least 8 characters");
        }
    }
}
