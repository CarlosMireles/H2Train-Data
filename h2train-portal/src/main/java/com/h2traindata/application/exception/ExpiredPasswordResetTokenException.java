package com.h2traindata.application.exception;

public class ExpiredPasswordResetTokenException extends RuntimeException {

    public ExpiredPasswordResetTokenException() {
        super("Password reset token has expired");
    }
}
