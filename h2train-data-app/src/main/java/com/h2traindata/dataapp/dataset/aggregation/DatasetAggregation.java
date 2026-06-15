package com.h2traindata.dataapp.dataset.aggregation;

import com.h2traindata.dataapp.dataset.exception.UnsupportedAggregationException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum DatasetAggregation {
    AVG("avg"),
    SUM("sum"),
    MIN("min"),
    MAX("max"),
    COUNT("count"),
    LATEST("latest");

    private final String value;

    DatasetAggregation(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static DatasetAggregation parse(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (DatasetAggregation aggregation : values()) {
                if (aggregation.value.equals(normalized)) {
                    return aggregation;
                }
            }
        }
        throw new UnsupportedAggregationException(value);
    }

    public static List<String> supportedValues() {
        return Arrays.stream(values()).map(DatasetAggregation::value).toList();
    }
}
