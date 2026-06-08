package com.h2traindata.dataapp.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.h2traindata.dataapp.application.TimeSeriesBuilderService;
import com.h2traindata.dataapp.application.TimeSeriesProjectionService;
import com.h2traindata.dataapp.application.TimeSeriesQueryService;
import com.h2traindata.dataapp.config.DataAppDatalakeProperties;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import com.h2traindata.dataapp.infrastructure.LongitudinalDatamartRepository;
import java.time.Instant;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiV1ControllerTest {

    private static final String USER_ID = "user-1";

    @TempDir
    private java.nio.file.Path tempDir;

    private ObjectMapper objectMapper;
    private TimeSeriesProjectionService projectionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        DataAppDatalakeProperties properties = new DataAppDatalakeProperties();
        properties.setRootPath(tempDir.resolve("datalake"));
        LongitudinalDatamartRepository datamartRepository =
                new LongitudinalDatamartRepository(properties, objectMapper);
        TimeSeriesBuilderService builderService = new TimeSeriesBuilderService();
        projectionService = new TimeSeriesProjectionService(builderService, datamartRepository, objectMapper);
        TimeSeriesQueryService queryService = new TimeSeriesQueryService(datamartRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(new ApiV1Controller(queryService))
                .setControllerAdvice(new DataAppExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void timeSeriesEndpointUsesVersionedPublicContract() throws Exception {
        projectionService.process(event("fitbit", "PHYSIOLOGICAL", "Steps", "steps-1",
                Instant.parse("2026-06-10T08:00:00Z"),
                node -> node.put("steps", 10500)));

        mockMvc.perform(get("/api/v1/users/{userId}/timeseries", USER_ID)
                        .param("metric", "daily_steps")
                        .param("from", "2026-06-10")
                        .param("to", "2026-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.metric").value("daily_steps"))
                .andExpect(jsonPath("$.unit").value("steps"))
                .andExpect(jsonPath("$.points[0].value").value(10500))
                .andExpect(jsonPath("$.points[0].sourceEventName").doesNotExist());
    }

    @Test
    void datasetExportReturnsDerivedDatasetWithoutPhysicalPaths() throws Exception {
        projectionService.process(event("fitbit", "PHYSIOLOGICAL", "Steps", "steps-1",
                Instant.parse("2026-06-10T08:00:00Z"),
                node -> node.put("steps", 10500)));

        mockMvc.perform(get("/api/v1/users/{userId}/dataset/export", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.metricCount").value(1))
                .andExpect(jsonPath("$.pointCount").value(1))
                .andExpect(jsonPath("$.timeseries.daily_steps[0].value").value(10500))
                .andExpect(jsonPath("$.datasetRoot").doesNotExist())
                .andExpect(jsonPath("$.subjectInfoFile").doesNotExist())
                .andExpect(jsonPath("$.timeseriesRoot").doesNotExist());
    }

    @Test
    void activitiesEndpointReturnsPagedActivityReadModel() throws Exception {
        projectionService.process(workout("run-1", "run", "2026-06-10T08:00:00Z", 1800, 5000, 400));
        projectionService.process(workout("bike-1", "bike", "2026-06-11T08:00:00Z", 3600, 20000, 800));

        mockMvc.perform(get("/api/v1/users/{userId}/activities", USER_ID)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    private NormalizedDatalakeEvent workout(String eventId,
                                            String activityType,
                                            String startTime,
                                            int duration,
                                            double distanceMeters,
                                            double calories) {
        return event("strava", "ACTIVITY", "Workout", eventId, Instant.parse(startTime),
                node -> {
                    node.put("activityType", activityType);
                    node.put("startTime", startTime);
                    node.put("duration", duration);
                    node.put("distanceMeters", distanceMeters);
                    node.put("calories", calories);
                });
    }

    private NormalizedDatalakeEvent event(String providerId,
                                          String eventType,
                                          String eventName,
                                          String eventId,
                                          Instant eventTimestamp,
                                          Consumer<ObjectNode> customizer) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("timestamp", eventTimestamp.toString());
        node.put("sourceSystem", providerId);
        customizer.accept(node);
        return new NormalizedDatalakeEvent(
                USER_ID,
                providerId,
                "athlete-1",
                eventType,
                eventName,
                eventId,
                eventTimestamp,
                eventTimestamp,
                node
        );
    }
}
