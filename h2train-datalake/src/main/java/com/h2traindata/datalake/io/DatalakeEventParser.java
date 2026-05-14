package com.h2traindata.datalake.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.datalake.domain.DatalakeEventRecord;
import com.h2traindata.datalake.domain.InvalidDatalakeEventException;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatalakeEventParser {

    private final ObjectMapper objectMapper;

    public DatalakeEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DatalakeEventRecord parse(String rawJson) {
        String sanitizedJson = removeByteOrderMark(rawJson);
        try {
            JsonNode root = objectMapper.readTree(sanitizedJson);
            String userId = requiredText(root, "userId");
            String providerId = requiredText(root, "providerId");
            String eventType = requiredText(root, "eventType");
            requiredText(root, "messageId");
            if (!root.hasNonNull("event") || !root.path("event").isObject()) {
                throw new InvalidDatalakeEventException(
                        "Bus message payload is not a bus event envelope: missing event object",
                        null
                );
            }
            Instant eventTimestamp = firstInstant(
                    root.at("/event/timestamp").asText(null),
                    root.path("publishedAt").asText(null)
            );
            return new DatalakeEventRecord(
                    sanitizedJson.strip(),
                    userId,
                    providerId,
                    eventType,
                    eventTimestamp != null ? eventTimestamp : Instant.now()
            );
        } catch (InvalidDatalakeEventException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new InvalidDatalakeEventException("Bus message payload is not valid JSON", exception);
        }
    }

    private Instant firstInstant(String... candidates) {
        for (String candidate : candidates) {
            Instant instant = toInstant(candidate);
            if (instant != null) {
                return instant;
            }
        }
        return null;
    }

    private Instant toInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private String requiredText(JsonNode root, String fieldName) {
        String value = root.path(fieldName).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new InvalidDatalakeEventException(
                    "Bus message payload is not a bus event envelope: missing " + fieldName,
                    null
            );
        }
        return value;
    }

    private String removeByteOrderMark(String rawJson) {
        if (rawJson == null) {
            return "";
        }
        if (rawJson.startsWith("\uFEFF")) {
            return rawJson.substring(1);
        }
        if (rawJson.startsWith("\u00EF\u00BB\u00BF")) {
            return rawJson.substring(3);
        }
        if (rawJson.startsWith("\u00C3\u00AF\u00C2\u00BB\u00C2\u00BF")) {
            return rawJson.substring(6);
        }
        return rawJson;
    }
}
