package com.h2traindata.dataapp.dataset.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h2traindata.dataapp.dataset.aggregation.AverageAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.CountAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.DatasetAggregationService;
import com.h2traindata.dataapp.dataset.aggregation.LatestAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.MaxAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.MinAggregationStrategy;
import com.h2traindata.dataapp.dataset.aggregation.SumAggregationStrategy;
import com.h2traindata.dataapp.dataset.format.CsvDatasetWriter;
import com.h2traindata.dataapp.dataset.format.DatasetRenderingService;
import com.h2traindata.dataapp.dataset.format.DatasetWriterFactory;
import com.h2traindata.dataapp.dataset.format.JsonDatasetWriter;
import com.h2traindata.dataapp.dataset.format.JsonlDatasetWriter;
import com.h2traindata.dataapp.dataset.service.DatasetCapabilitiesService;
import com.h2traindata.dataapp.dataset.service.DatasetExportService;
import com.h2traindata.dataapp.dataset.service.DatasetQueryService;
import com.h2traindata.dataapp.dataset.service.DatasetRequestValidator;
import com.h2traindata.dataapp.dataset.service.HeartRateZoneDatasetService;
import com.h2traindata.dataapp.dataset.service.LongitudinalDatasetReader;
import com.h2traindata.dataapp.domain.AggregationType;
import com.h2traindata.dataapp.domain.SubjectInfo;
import com.h2traindata.dataapp.domain.TimeSeriesPoint;
import com.h2traindata.dataapp.web.DataAppExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DatasetQueryControllerTest {

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        LongitudinalDatasetReader reader = new TestReader();
        DatasetAggregationService aggregationService = new DatasetAggregationService(List.of(
                new AverageAggregationStrategy(),
                new SumAggregationStrategy(),
                new MinAggregationStrategy(),
                new MaxAggregationStrategy(),
                new CountAggregationStrategy(),
                new LatestAggregationStrategy()
        ));
        DatasetRequestValidator validator = new DatasetRequestValidator(reader);
        DatasetQueryService queryService = new DatasetQueryService(reader, validator, aggregationService);
        DatasetExportService exportService = new DatasetExportService(reader, validator, aggregationService);
        DatasetWriterFactory writerFactory = new DatasetWriterFactory(List.of(
                new JsonDatasetWriter(objectMapper),
                new JsonlDatasetWriter(objectMapper),
                new CsvDatasetWriter()
        ));
        DatasetQueryController controller = new DatasetQueryController(
                queryService,
                exportService,
                new DatasetCapabilitiesService(reader),
                new DatasetRenderingService(writerFactory),
                new HeartRateZoneDatasetService(reader)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new DataAppExceptionHandler())
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void returnsValidJsonQuery() throws Exception {
        mockMvc.perform(get("/api/v1/datasets/query")
                        .param("metric", "daily_calories")
                        .param("operator", "gt")
                        .param("value", "2500")
                        .param("aggregation", "avg")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-02")
                        .param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subjects[0].userId").value("u001"))
                .andExpect(jsonPath("$.subjects[0].aggregatedValue").value(2700));
    }

    @Test
    void returnsBadRequestForInvalidBetweenQuery() throws Exception {
        mockMvc.perform(get("/api/v1/datasets/query")
                        .param("metric", "daily_calories")
                        .param("operator", "between")
                        .param("value", "2500")
                        .param("aggregation", "avg")
                        .param("format", "json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("maxValue is required for between"));
    }

    @Test
    void returnsQueryCapabilities() throws Exception {
        mockMvc.perform(get("/api/v1/datasets/query/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics[0]").value("daily_calories"))
                .andExpect(jsonPath("$.operators[0]").value("gt"))
                .andExpect(jsonPath("$.aggregations[0]").value("avg"))
                .andExpect(jsonPath("$.formats[2]").value("csv"))
                .andExpect(jsonPath("$.dimensions").isArray());
    }

    @Test
    void exportsFilteredDatasetAsCsv() throws Exception {
        String request = """
                {
                  "metrics": ["daily_steps", "daily_calories"],
                  "filters": [{
                    "metric": "daily_calories",
                    "operator": "gt",
                    "value": 2500,
                    "aggregation": "avg"
                  }],
                  "from": "2026-01-01",
                  "to": "2026-01-02",
                  "format": "csv"
                }
                """;

        mockMvc.perform(post("/api/v1/datasets/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"h2train-dataset.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "u001,daily_steps,10000,2026-01-01T00:00:00Z")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("u002,daily_steps"))));
    }

    @Test
    void filtersQueryByHeartRateZone() throws Exception {
        mockMvc.perform(get("/api/v1/datasets/query")
                        .param("metric", "heart_rate_zone_minutes")
                        .param("operator", "gt")
                        .param("value", "30")
                        .param("aggregation", "sum")
                        .param("zone", "cardio")
                        .param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(1))
                .andExpect(jsonPath("$.subjects[0].userId").value("u001"))
                .andExpect(jsonPath("$.subjects[0].aggregatedValue").value(40));
    }

    @Test
    void returnsDailyHeartRateZoneDataset() throws Exception {
        mockMvc.perform(get("/api/v1/datasets/heart-rate-zones")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-01")
                        .param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].userId").value("u001"))
                .andExpect(jsonPath("$.days[0].provider").value("test"))
                .andExpect(jsonPath("$.days[0].trackedMinutes").value(1440))
                .andExpect(jsonPath("$.days[0].activeMinutes").value(40))
                .andExpect(jsonPath("$.days[0].highIntensityMinutes").value(40))
                .andExpect(jsonPath("$.days[0].dominantActiveZone").value("cardio"));
    }

    private static final class TestReader implements LongitudinalDatasetReader {

        private final List<SubjectInfo> subjects = List.of(subject("u001"), subject("u002"));
        private final Map<String, Map<String, List<TimeSeriesPoint>>> points = Map.of(
                "u001", Map.of(
                        "daily_calories", List.of(
                                point("u001", "daily_calories", "2026-01-01T00:00:00Z", "2600"),
                                point("u001", "daily_calories", "2026-01-02T00:00:00Z", "2800")),
                        "daily_steps", List.of(
                                point("u001", "daily_steps", "2026-01-01T00:00:00Z", "10000")),
                        "heart_rate_zone_minutes", List.of(
                                point("u001", "heart_rate_zone_minutes", "2026-01-01T00:00:00Z",
                                        "40", "cardio"),
                                point("u001", "heart_rate_zone_minutes", "2026-01-01T00:00:00Z",
                                        "1400", "out_of_range")),
                        "heart_rate_zone_calories", List.of(
                                point("u001", "heart_rate_zone_calories", "2026-01-01T00:00:00Z",
                                        "300", "cardio"),
                                point("u001", "heart_rate_zone_calories", "2026-01-01T00:00:00Z",
                                        "1200", "out_of_range"))),
                "u002", Map.of(
                        "daily_calories", List.of(
                                point("u002", "daily_calories", "2026-01-01T00:00:00Z", "2000")),
                        "daily_steps", List.of(
                                point("u002", "daily_steps", "2026-01-01T00:00:00Z", "12000")),
                        "heart_rate_zone_minutes", List.of(
                                point("u002", "heart_rate_zone_minutes", "2026-01-01T00:00:00Z",
                                        "20", "cardio")),
                        "heart_rate_zone_calories", List.of(
                                point("u002", "heart_rate_zone_calories", "2026-01-01T00:00:00Z",
                                        "150", "cardio")))
        );

        @Override
        public List<SubjectInfo> subjects() {
            return subjects;
        }

        @Override
        public Set<String> availableMetrics() {
            return Set.of(
                    "daily_calories",
                    "daily_steps",
                    "heart_rate_zone_minutes",
                    "heart_rate_zone_calories"
            );
        }

        @Override
        public List<TimeSeriesPoint> readPoints(String userId, String metric, LocalDate from, LocalDate to) {
            return points.getOrDefault(userId, Map.of())
                    .getOrDefault(metric, List.of())
                    .stream()
                    .filter(point -> {
                        LocalDate date = point.periodStart().atZone(ZoneOffset.UTC).toLocalDate();
                        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
                    })
                    .toList();
        }

        private static SubjectInfo subject(String userId) {
            return new SubjectInfo(userId, userId, Set.of(), "Z", null, null, null, null, null);
        }

        private static TimeSeriesPoint point(String userId, String metric, String startValue, String value) {
            return point(userId, metric, startValue, value, null);
        }

        private static TimeSeriesPoint point(String userId,
                                             String metric,
                                             String startValue,
                                             String value,
                                             String zone) {
            Instant start = Instant.parse(startValue);
            return new TimeSeriesPoint(
                    userId,
                    metric,
                    "P1D",
                    start,
                    start.plusSeconds(86400),
                    new BigDecimal(value),
                    metric.equals("daily_steps") ? "steps"
                            : metric.equals("heart_rate_zone_minutes") ? "min" : "kcal",
                    "test",
                    null,
                    null,
                    AggregationType.SUM,
                    null,
                    zone,
                    start
            );
        }
    }
}
