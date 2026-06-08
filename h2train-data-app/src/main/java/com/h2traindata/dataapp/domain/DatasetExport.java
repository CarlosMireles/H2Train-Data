package com.h2traindata.dataapp.domain;

import java.nio.file.Path;
import java.util.List;

public record DatasetExport(
        Path datasetRoot,
        Path subjectInfoFile,
        Path timeseriesRoot,
        int subjectCount,
        int pointCount,
        List<String> metrics
) {
}
