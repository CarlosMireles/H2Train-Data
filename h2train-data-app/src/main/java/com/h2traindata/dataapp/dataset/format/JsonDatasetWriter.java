package com.h2traindata.dataapp.dataset.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.dataapp.dataset.dto.DatasetExportResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryResponse;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetResponse;
import org.springframework.stereotype.Component;

@Component
public class JsonDatasetWriter implements DatasetWriter {

    private final ObjectMapper objectMapper;

    public JsonDatasetWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DatasetFormat format() {
        return DatasetFormat.JSON;
    }

    @Override
    public byte[] writeQuery(DatasetQueryResponse response) {
        return write(response);
    }

    @Override
    public byte[] writeExport(DatasetExportResponse response) {
        return write(response);
    }

    @Override
    public byte[] writeHeartRateZones(HeartRateZoneDatasetResponse response) {
        return write(response);
    }

    private byte[] write(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize dataset as JSON", exception);
        }
    }
}
