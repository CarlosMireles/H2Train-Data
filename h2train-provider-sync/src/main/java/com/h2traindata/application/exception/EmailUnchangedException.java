package com.h2traindata.application.exception;

public class EmailUnchangedException extends RuntimeException {

    public EmailUnchangedException() {
        super("New email must be different from the current email");
    }
}
