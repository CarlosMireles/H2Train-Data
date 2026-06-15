package com.h2traindata.dataapp.dataset.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h2traindata.dataapp.dataset.dto.DatasetExportRequest;
import com.h2traindata.dataapp.dataset.dto.DatasetExportResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetExportRow;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryRequest;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetSubjectMatch;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetRequest;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetResponse;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDay;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneValue;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasetWriterTest {

    private ObjectMapper objectMapper;
    private DatasetQueryResponse queryResponse;
    private DatasetExportResponse exportResponse;
    private HeartRateZoneDatasetResponse heartRateZoneResponse;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        LocalDate from = LocalDate.parse("2026-01-01");
        LocalDate to = LocalDate.parse("2026-01-31");
        queryResponse = new DatasetQueryResponse(
                new DatasetQueryRequest("daily_calories", "gt", BigDecimal.valueOf(2500), null,
                        "avg", null, from, to, "json"),
                List.of(new DatasetSubjectMatch("u001", "daily_calories", BigDecimal.valueOf(2840.5), from, to))
        );
        exportResponse = new DatasetExportResponse(
                new DatasetExportRequest(List.of("daily_steps"), List.of(), null, from, to, "csv"),
                1,
                1,
                List.of(new DatasetExportRow(
                        "u001",
                        "daily_steps",
                        BigDecimal.valueOf(10000),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-02T00:00:00Z"),
                        "steps",
                        "P1D",
                        "fitbit",
                        null,
                        null
                ))
        );
        heartRateZoneResponse = new HeartRateZoneDatasetResponse(
                new HeartRateZoneDatasetRequest(from, to, List.of(), List.of(), "csv"),
                List.of(new HeartRateZoneDay(
                        "u001",
                        from,
                        "fitbit",
                        BigDecimal.valueOf(1440),
                        BigDecimal.valueOf(40),
                        BigDecimal.valueOf(1500),
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(40),
                        "cardio",
                        List.of(new HeartRateZoneValue(
                                "cardio",
                                BigDecimal.valueOf(40),
                                BigDecimal.valueOf(300),
                                BigDecimal.valueOf(2.78),
                                BigDecimal.valueOf(100)
                        ))
                ))
        );
    }

    @Test
    void writesJson() throws Exception {
        byte[] output = new JsonDatasetWriter(objectMapper).writeQuery(queryResponse);

        assertEquals("u001", objectMapper.readTree(output).path("subjects").get(0).path("userId").asText());
    }

    @Test
    void writesJsonl() {
        String output = text(new JsonlDatasetWriter(objectMapper).writeQuery(queryResponse));

        assertEquals(1, output.lines().count());
        assertTrue(output.contains("\"aggregatedValue\":2840.5"));
    }

    @Test
    void writesCsv() {
        String output = text(new CsvDatasetWriter().writeExport(exportResponse));

        assertTrue(output.startsWith(
                "userId,metric,value,periodStart,periodEnd,unit,period,provider,activityType,zone\n"));
        assertTrue(output.contains("u001,daily_steps,10000,2026-01-01T00:00:00Z"));
    }

    @Test
    void writesHeartRateZonesAsJsonl() {
        String output = text(new JsonlDatasetWriter(objectMapper).writeHeartRateZones(heartRateZoneResponse));

        assertEquals(1, output.lines().count());
        assertTrue(output.contains("\"zone\":\"cardio\""));
        assertTrue(output.contains("\"activeMinutes\":40"));
    }

    @Test
    void writesHeartRateZonesAsCsv() {
        String output = text(new CsvDatasetWriter().writeHeartRateZones(heartRateZoneResponse));

        assertTrue(output.startsWith("userId,date,provider,trackedMinutes,activeMinutes"));
        assertTrue(output.contains("u001,2026-01-01,fitbit,1440,40,1500,300,40,cardio,cardio,40,300,2.78,100"));
    }

    private String text(byte[] value) {
        return new String(value, StandardCharsets.UTF_8);
    }
}
