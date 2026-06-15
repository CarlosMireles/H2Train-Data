package com.h2traindata.dataapp.dataset.format;

import com.h2traindata.dataapp.dataset.exception.UnsupportedDatasetFormatException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum DatasetFormat {
    JSON("json", "application/json", "json"),
    JSONL("jsonl", "application/x-ndjson", "jsonl"),
    CSV("csv", "text/csv", "csv");

    private final String value;
    private final String mediaType;
    private final String extension;

    DatasetFormat(String value, String mediaType, String extension) {
        this.value = value;
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String value() {
        return value;
    }

    public String mediaType() {
        return mediaType;
    }

    public String extension() {
        return extension;
    }

    public static DatasetFormat parse(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (DatasetFormat format : values()) {
                if (format.value.equals(normalized)) {
                    return format;
                }
            }
        }
        throw new UnsupportedDatasetFormatException(value);
    }

    public static List<String> supportedValues() {
        return Arrays.stream(values()).map(DatasetFormat::value).toList();
    }
}
