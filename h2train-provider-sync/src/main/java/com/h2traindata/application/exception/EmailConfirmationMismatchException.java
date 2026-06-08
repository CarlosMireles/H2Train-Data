package com.h2traindata.application.exception;

public class EmailConfirmationMismatchException extends RuntimeException {

    public EmailConfirmationMismatchException() {
        super("Email confirmation does not match");
    }
}
