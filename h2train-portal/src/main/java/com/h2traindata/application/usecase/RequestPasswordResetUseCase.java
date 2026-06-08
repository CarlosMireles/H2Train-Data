package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.AccountManagedExternallyException;
import com.h2traindata.application.exception.UserAccountNotFoundException;
import com.h2traindata.application.port.out.PasswordResetMessageSender;
import com.h2traindata.application.port.out.PasswordResetTokenRepository;
import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.PasswordResetTokenService;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.PasswordResetToken;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestPasswordResetUseCase {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetMessageSender passwordResetMessageSender;
    private final PasswordResetTokenService passwordResetTokenService;
    private final Clock clock;

    @Autowired
    public RequestPasswordResetUseCase(UserAccountRepository userAccountRepository,
                                       PasswordResetTokenRepository passwordResetTokenRepository,
                                       PasswordResetMessageSender passwordResetMessageSender,
                                       PasswordResetTokenService passwordResetTokenService) {
        this(
                userAccountRepository,
                passwordResetTokenRepository,
                passwordResetMessageSender,
                passwordResetTokenService,
                Clock.systemUTC()
        );
    }

    RequestPasswordResetUseCase(UserAccountRepository userAccountRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                PasswordResetMessageSender passwordResetMessageSender,
                                PasswordResetTokenService passwordResetTokenService,
                                Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetMessageSender = passwordResetMessageSender;
        this.passwordResetTokenService = passwordResetTokenService;
        this.clock = clock;
    }

    public void execute(String userId, String resetUrl) {
        InternalUserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UserAccountNotFoundException(userId));
        if (!userAccount.hasLocalCredentials()) {
            throw new AccountManagedExternallyException();
        }
        createResetToken(userAccount, resetUrl);
    }

    public void executeForEmail(String email, String resetUrl) {
        if (email == null || email.isBlank()) {
            return;
        }
        userAccountRepository.findByEmail(email.trim())
                .filter(InternalUserAccount::hasLocalCredentials)
                .ifPresent(userAccount -> createResetToken(userAccount, resetUrl));
    }

    private void createResetToken(InternalUserAccount userAccount, String resetUrl) {
        String rawToken = passwordResetTokenService.generateToken();
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(TOKEN_TTL);

        passwordResetTokenRepository.save(new PasswordResetToken(
                passwordResetTokenService.hashToken(rawToken),
                userAccount.id(),
                userAccount.email(),
                expiresAt,
                now,
                null
        ));

        passwordResetMessageSender.sendPasswordReset(
                userAccount.email(),
                userAccount.username(),
                resetUri(resetUrl, rawToken),
                expiresAt
        );
    }

    private URI resetUri(String resetUrl, String rawToken) {
        String separator = resetUrl.contains("?") ? "&" : "?";
        return URI.create(resetUrl + separator + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
    }
}
