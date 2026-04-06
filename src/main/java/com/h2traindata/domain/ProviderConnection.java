package com.h2traindata.domain;

import java.time.Instant;

public record ProviderConnection(
        String providerId,
        AthleteProfile athlete,
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {
}
