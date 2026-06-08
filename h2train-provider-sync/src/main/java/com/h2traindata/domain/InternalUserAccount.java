package com.h2traindata.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record InternalUserAccount(
        String id,
        String email,
        String username,
        String passwordHash,
        Set<String> providerIds,
        Instant createdAt
) {

    public InternalUserAccount {
        providerIds = providerIds == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(providerIds));
    }

    public InternalUserAccount(String id, Instant createdAt) {
        this(id, null, null, null, Set.of(), createdAt);
    }

    public InternalUserAccount withProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return this;
        }

        LinkedHashSet<String> updatedProviderIds = new LinkedHashSet<>(providerIds);
        updatedProviderIds.add(providerId);
        return new InternalUserAccount(id, email, username, passwordHash, updatedProviderIds, createdAt);
    }

    public InternalUserAccount withEmail(String email) {
        return new InternalUserAccount(id, email, username, passwordHash, providerIds, createdAt);
    }

    public InternalUserAccount withPasswordHash(String passwordHash) {
        return new InternalUserAccount(id, email, username, passwordHash, providerIds, createdAt);
    }
}
