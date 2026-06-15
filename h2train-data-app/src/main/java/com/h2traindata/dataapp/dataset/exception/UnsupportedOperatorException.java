package com.h2traindata.dataapp.dataset.exception;

public class UnsupportedOperatorException extends InvalidDatasetQueryException {

    public UnsupportedOperatorException(String operator) {
        super("Unsupported dataset operator: " + operator);
    }
}
