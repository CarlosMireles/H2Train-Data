package com.h2traindata.dataapp.dataset.exception;

public class UnsupportedMetricException extends RuntimeException {

    public UnsupportedMetricException(String metric) {
        super("Metric not found: " + metric);
    }
}
