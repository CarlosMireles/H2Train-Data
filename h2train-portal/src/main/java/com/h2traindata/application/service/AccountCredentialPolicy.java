package com.h2traindata.application.service;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccountCredentialPolicy {

    public static final int MINIMUM_PASSWORD_LENGTH = 8;

    public String required(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public String normalizeEmail(String email) {
        String normalized = required("email", email).toLowerCase(Locale.ROOT);
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("email must be valid");
        }
        return normalized;
    }

    public void validatePassword(String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("password must have at least 8 characters");
        }
    }
}
