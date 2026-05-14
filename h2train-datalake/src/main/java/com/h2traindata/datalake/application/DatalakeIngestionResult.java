package com.h2traindata.datalake.application;

import com.h2traindata.datalake.domain.DatalakeEventRecord;
import java.nio.file.Path;

public record DatalakeIngestionResult(
        boolean successful,
        Path file,
        DatalakeEventRecord eventRecord,
        String failureReason
) {

    public static DatalakeIngestionResult success(Path file, DatalakeEventRecord eventRecord) {
        return new DatalakeIngestionResult(true, file, eventRecord, null);
    }

    public static DatalakeIngestionResult failure(Path file, RuntimeException exception) {
        return new DatalakeIngestionResult(false, file, null, exception.getMessage());
    }
}
