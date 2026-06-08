package com.h2traindata.application.exception;

public class PasswordConfirmationMismatchException extends RuntimeException {

    public PasswordConfirmationMismatchException() {
        super("Password confirmation does not match");
    }
}
