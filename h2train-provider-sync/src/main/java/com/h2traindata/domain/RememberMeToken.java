package com.h2traindata.domain;

import java.time.Instant;

public record RememberMeToken(
        String tokenHash,
        String userId,
        Instant expiresAt,
        Instant createdAt,
        Instant lastUsedAt
) {

    public RememberMeToken markUsed(Instant lastUsedAt) {
        return new RememberMeToken(tokenHash, userId, expiresAt, createdAt, lastUsedAt);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
