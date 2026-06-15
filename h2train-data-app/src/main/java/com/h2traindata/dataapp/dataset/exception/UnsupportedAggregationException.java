package com.h2traindata.dataapp.dataset.exception;

public class UnsupportedAggregationException extends InvalidDatasetQueryException {

    public UnsupportedAggregationException(String aggregation) {
        super("Unsupported dataset aggregation: " + aggregation);
    }
}
