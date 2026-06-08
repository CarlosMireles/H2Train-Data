package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.PasswordResetTokenRepository;
import com.h2traindata.domain.PasswordResetToken;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "type", havingValue = "memory")
public class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {

    private final ConcurrentMap<String, PasswordResetToken> tokens = new ConcurrentHashMap<>();

    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        tokens.put(passwordResetToken.tokenHash(), passwordResetToken);
        return passwordResetToken;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(tokens.get(tokenHash));
    }
}
