package com.h2traindata.application.exception;

public class AccountManagedExternallyException extends RuntimeException {

    public AccountManagedExternallyException() {
        super("This account is managed by an external identity provider");
    }
}
