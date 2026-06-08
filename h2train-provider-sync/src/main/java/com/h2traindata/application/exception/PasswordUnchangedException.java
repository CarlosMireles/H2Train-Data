package com.h2traindata.application.exception;

public class PasswordUnchangedException extends RuntimeException {

    public PasswordUnchangedException() {
        super("New password must be different from the current password");
    }
}
