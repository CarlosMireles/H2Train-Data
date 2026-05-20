package com.h2traindata.application.exception;

public class DuplicateUserAccountException extends RuntimeException {

    public DuplicateUserAccountException(String fieldName) {
        super("An account already exists with this " + fieldName);
    }
}
