package com.h2traindata.domain;

import java.time.Instant;

public record PasswordResetToken(
        String tokenHash,
        String userId,
        String email,
        Instant expiresAt,
        Instant createdAt,
        Instant usedAt
) {

    public PasswordResetToken markUsed(Instant usedAt) {
        return new PasswordResetToken(tokenHash, userId, email, expiresAt, createdAt, usedAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
