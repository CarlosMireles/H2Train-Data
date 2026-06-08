package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.RememberMeTokenRepository;
import com.h2traindata.domain.RememberMeToken;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "type", havingValue = "memory")
public class InMemoryRememberMeTokenRepository implements RememberMeTokenRepository {

    private final ConcurrentMap<String, RememberMeToken> tokens = new ConcurrentHashMap<>();

    @Override
    public RememberMeToken save(RememberMeToken rememberMeToken) {
        tokens.put(rememberMeToken.tokenHash(), rememberMeToken);
        return rememberMeToken;
    }

    @Override
    public Optional<RememberMeToken> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(tokens.get(tokenHash));
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        tokens.remove(tokenHash);
    }
}
