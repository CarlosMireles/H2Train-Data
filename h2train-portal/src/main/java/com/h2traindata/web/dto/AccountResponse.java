package com.h2traindata.web.dto;

import java.util.Set;

public record AccountResponse(
        String userId,
        String email,
        String username,
        Set<String> providers
) {
}
