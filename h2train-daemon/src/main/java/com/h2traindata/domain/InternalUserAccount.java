package com.h2traindata.domain;

import java.time.Instant;

public record InternalUserAccount(
        String id,
        Instant createdAt
) {
}
