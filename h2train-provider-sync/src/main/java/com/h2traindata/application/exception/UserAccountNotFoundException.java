package com.h2traindata.application.exception;

public class UserAccountNotFoundException extends RuntimeException {

    public UserAccountNotFoundException(String userId) {
        super("User account not found: " + userId);
    }
}
