package com.h2traindata.dataapp.dataset.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.dataapp.dataset.dto.DatasetExportResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryResponse;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JsonlDatasetWriter implements DatasetWriter {

    private final ObjectMapper objectMapper;

    public JsonlDatasetWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DatasetFormat format() {
        return DatasetFormat.JSONL;
    }

    @Override
    public byte[] writeQuery(DatasetQueryResponse response) {
        return writeLines(response.subjects());
    }

    @Override
    public byte[] writeExport(DatasetExportResponse response) {
        return writeLines(response.rows());
    }

    @Override
    public byte[] writeHeartRateZones(HeartRateZoneDatasetResponse response) {
        return writeLines(HeartRateZoneDatasetRows.flatten(response));
    }

    private byte[] writeLines(List<?> values) {
        try {
            StringBuilder output = new StringBuilder();
            for (Object value : values) {
                output.append(objectMapper.writeValueAsString(value)).append('\n');
            }
            return output.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize dataset as JSONL", exception);
        }
    }
}
