package com.h2traindata.dataapp.dataset.format;

import com.h2traindata.dataapp.dataset.service.DatasetExportResult;
import com.h2traindata.dataapp.dataset.service.DatasetQueryResult;
import com.h2traindata.dataapp.dataset.service.HeartRateZoneDatasetResult;
import org.springframework.stereotype.Service;

@Service
public class DatasetRenderingService {

    private final DatasetWriterFactory writerFactory;

    public DatasetRenderingService(DatasetWriterFactory writerFactory) {
        this.writerFactory = writerFactory;
    }

    public DatasetRenderedResponse renderQuery(DatasetQueryResult result) {
        DatasetWriter writer = writerFactory.writerFor(result.format());
        String filename = result.format() == DatasetFormat.JSON
                ? null
                : "h2train-dataset-query." + result.format().extension();
        return new DatasetRenderedResponse(
                writer.writeQuery(result.response()),
                result.format().mediaType(),
                filename
        );
    }

    public DatasetRenderedResponse renderExport(DatasetExportResult result) {
        DatasetWriter writer = writerFactory.writerFor(result.format());
        return new DatasetRenderedResponse(
                writer.writeExport(result.response()),
                result.format().mediaType(),
                "h2train-dataset." + result.format().extension()
        );
    }

    public DatasetRenderedResponse renderHeartRateZones(HeartRateZoneDatasetResult result) {
        DatasetWriter writer = writerFactory.writerFor(result.format());
        String filename = result.format() == DatasetFormat.JSON
                ? null
                : "h2train-heart-rate-zones." + result.format().extension();
        return new DatasetRenderedResponse(
                writer.writeHeartRateZones(result.response()),
                result.format().mediaType(),
                filename
        );
    }
}
