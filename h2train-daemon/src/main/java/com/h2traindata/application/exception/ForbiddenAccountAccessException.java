package com.h2traindata.application.exception;

public class ForbiddenAccountAccessException extends RuntimeException {

    public ForbiddenAccountAccessException() {
        super("The requested resource does not belong to the authenticated account");
    }
}
