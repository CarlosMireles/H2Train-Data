package com.h2traindata.dataapp.dataset.filter;

import com.h2traindata.dataapp.dataset.exception.InvalidDatasetQueryException;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum DatasetDimension {
    ZONE("zone") {
        @Override
        public String valueFrom(TimeSeriesPoint point) {
            return point.zone();
        }
    },
    ACTIVITY_TYPE("activityType") {
        @Override
        public String valueFrom(TimeSeriesPoint point) {
            return point.activityType();
        }
    },
    PROVIDER("provider") {
        @Override
        public String valueFrom(TimeSeriesPoint point) {
            return point.provider();
        }
    };

    private final String value;

    DatasetDimension(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public abstract String valueFrom(TimeSeriesPoint point);

    public static DatasetDimension parse(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT).replace("_", "");
            for (DatasetDimension dimension : values()) {
                if (dimension.value.toLowerCase(Locale.ROOT).equals(normalized)) {
                    return dimension;
                }
            }
        }
        throw new InvalidDatasetQueryException("Unsupported dataset dimension: " + value);
    }

    public static List<String> supportedValues() {
        return Arrays.stream(values()).map(DatasetDimension::value).toList();
    }
}
