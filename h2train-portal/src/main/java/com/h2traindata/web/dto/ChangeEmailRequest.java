package com.h2traindata.web.dto;

public record ChangeEmailRequest(
        String newEmail,
        String confirmNewEmail,
        String currentPassword
) {
}
