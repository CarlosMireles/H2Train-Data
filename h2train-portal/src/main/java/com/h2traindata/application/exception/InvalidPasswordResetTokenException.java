package com.h2traindata.application.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {

    public InvalidPasswordResetTokenException() {
        super("Password reset token is not valid");
    }
}
