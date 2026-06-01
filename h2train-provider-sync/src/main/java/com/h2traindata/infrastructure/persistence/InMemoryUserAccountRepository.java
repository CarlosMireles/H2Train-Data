package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.UserAccountRepository;
import com.h2traindata.domain.InternalUserAccount;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "type", havingValue = "memory")
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final ConcurrentMap<String, InternalUserAccount> userAccounts = new ConcurrentHashMap<>();

    @Override
    public InternalUserAccount save(InternalUserAccount userAccount) {
        userAccounts.put(userAccount.id(), userAccount);
        return userAccount;
    }

    @Override
    public Optional<InternalUserAccount> findById(String userId) {
        return Optional.ofNullable(userAccounts.get(userId));
    }

    @Override
    public Optional<InternalUserAccount> findByEmail(String email) {
        String normalizedEmail = normalize(email);
        return userAccounts.values().stream()
                .filter(userAccount -> normalizedEmail.equals(normalize(userAccount.email())))
                .findFirst();
    }

    @Override
    public Optional<InternalUserAccount> findByUsername(String username) {
        String normalizedUsername = normalize(username);
        return userAccounts.values().stream()
                .filter(userAccount -> normalizedUsername.equals(normalize(userAccount.username())))
                .findFirst();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
