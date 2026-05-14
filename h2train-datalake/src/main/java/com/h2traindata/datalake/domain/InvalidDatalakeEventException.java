package com.h2traindata.datalake.domain;

public class InvalidDatalakeEventException extends RuntimeException {

    public InvalidDatalakeEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
