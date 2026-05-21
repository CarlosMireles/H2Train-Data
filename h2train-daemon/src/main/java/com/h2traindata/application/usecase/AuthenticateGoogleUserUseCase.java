package com.h2traindata.application.usecase;

import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.application.service.AccountEventPublisher;
import com.h2traindata.domain.InternalUserAccount;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthenticateGoogleUserUseCase {

    private final UserAccountRepository userAccountRepository;
    private final AccountEventPublisher accountEventPublisher;

    public AuthenticateGoogleUserUseCase(UserAccountRepository userAccountRepository,
                                         AccountEventPublisher accountEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.accountEventPublisher = accountEventPublisher;
    }

    public InternalUserAccount execute(String email, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        return userAccountRepository.findByEmail(normalizedEmail)
                .map(existingUserAccount -> {
                    accountEventPublisher.publishUserLoggedIn(existingUserAccount, "google");
                    return existingUserAccount;
                })
                .orElseGet(() -> {
                    InternalUserAccount userAccount = userAccountRepository.save(new InternalUserAccount(
                            UUID.randomUUID().toString(),
                            normalizedEmail,
                            uniqueUsername(normalizedEmail, displayName),
                            null,
                            Set.of(),
                            Instant.now()
                    ));
                    accountEventPublisher.publishUserRegistered(userAccount, "google");
                    return userAccount;
                });
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new IllegalArgumentException("Google account did not return a valid email");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String uniqueUsername(String email, String displayName) {
        String base = usernameBase(email, displayName);
        String candidate = base;
        int suffix = 2;
        while (userAccountRepository.findByUsername(candidate).isPresent()) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String usernameBase(String email, String displayName) {
        String source = StringUtils.hasText(displayName) ? displayName : email.substring(0, email.indexOf('@'));
        String normalized = source.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return StringUtils.hasText(normalized) ? normalized : "user";
    }
}
