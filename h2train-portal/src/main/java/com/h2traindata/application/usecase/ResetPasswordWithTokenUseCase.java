package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.AccountManagedExternallyException;
import com.h2traindata.application.exception.ExpiredPasswordResetTokenException;
import com.h2traindata.application.exception.InvalidPasswordResetTokenException;
import com.h2traindata.application.exception.PasswordConfirmationMismatchException;
import com.h2traindata.application.exception.PasswordUnchangedException;
import com.h2traindata.application.exception.UserAccountNotFoundException;
import com.h2traindata.application.port.out.PasswordHasher;
import com.h2traindata.application.port.out.PasswordResetTokenRepository;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountCredentialPolicy;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.application.service.PasswordResetTokenService;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.PasswordResetToken;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResetPasswordWithTokenUseCase {

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordHasher passwordHasher;
    private final AccountCredentialPolicy accountCredentialPolicy;
    private final AccountEventPublisher accountEventPublisher;
    private final PasswordResetTokenService passwordResetTokenService;
    private final Clock clock;

    @Autowired
    public ResetPasswordWithTokenUseCase(UserAccountRepository userAccountRepository,
                                         PasswordResetTokenRepository passwordResetTokenRepository,
                                         PasswordHasher passwordHasher,
                                         AccountCredentialPolicy accountCredentialPolicy,
                                         AccountEventPublisher accountEventPublisher,
                                         PasswordResetTokenService passwordResetTokenService) {
        this(
                userAccountRepository,
                passwordResetTokenRepository,
                passwordHasher,
                accountCredentialPolicy,
                accountEventPublisher,
                passwordResetTokenService,
                Clock.systemUTC()
        );
    }

    ResetPasswordWithTokenUseCase(UserAccountRepository userAccountRepository,
                                  PasswordResetTokenRepository passwordResetTokenRepository,
                                  PasswordHasher passwordHasher,
                                  AccountCredentialPolicy accountCredentialPolicy,
                                  AccountEventPublisher accountEventPublisher,
                                  PasswordResetTokenService passwordResetTokenService,
                                  Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordHasher = passwordHasher;
        this.accountCredentialPolicy = accountCredentialPolicy;
        this.accountEventPublisher = accountEventPublisher;
        this.passwordResetTokenService = passwordResetTokenService;
        this.clock = clock;
    }

    public InternalUserAccount execute(String token, String newPassword, String confirmNewPassword) {
        Instant now = Instant.now(clock);
        PasswordResetToken passwordResetToken = passwordResetTokenRepository
                .findByTokenHash(passwordResetTokenService.hashToken(token))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        if (passwordResetToken.isUsed()) {
            throw new InvalidPasswordResetTokenException();
        }
        if (passwordResetToken.isExpired(now)) {
            throw new ExpiredPasswordResetTokenException();
        }

        accountCredentialPolicy.validatePassword(newPassword);
        if (!newPassword.equals(confirmNewPassword)) {
            throw new PasswordConfirmationMismatchException();
        }

        InternalUserAccount userAccount = userAccountRepository.findById(passwordResetToken.userId())
                .orElseThrow(() -> new UserAccountNotFoundException(passwordResetToken.userId()));
        if (!userAccount.hasLocalCredentials()) {
            throw new AccountManagedExternallyException();
        }
        if (passwordHasher.matches(newPassword, userAccount.passwordHash())) {
            throw new PasswordUnchangedException();
        }

        InternalUserAccount updated = userAccountRepository.save(userAccount.withPasswordHash(passwordHasher.hash(newPassword)));
        passwordResetTokenRepository.save(passwordResetToken.markUsed(now));
        accountEventPublisher.publishUserPasswordChanged(updated);
        return updated;
    }
}
