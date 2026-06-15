package com.h2traindata.dataapp.dataset.exception;

public class UnsupportedDatasetFormatException extends InvalidDatasetQueryException {

    public UnsupportedDatasetFormatException(String format) {
        super("Unsupported dataset format: " + format);
    }
}
