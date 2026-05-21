package com.h2traindata.domain;

public record EventPublication(
        String userId,
        ProviderEvent event
) {
}
