package com.h2traindata.application.exception;

public class EmailAlreadyInUseException extends RuntimeException {

    public EmailAlreadyInUseException() {
        super("An account already exists with this email");
    }
}
