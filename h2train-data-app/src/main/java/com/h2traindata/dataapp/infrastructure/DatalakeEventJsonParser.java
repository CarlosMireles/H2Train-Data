package com.h2traindata.dataapp.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatalakeEventJsonParser {

    private final ObjectMapper objectMapper;

    public DatalakeEventJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<NormalizedDatalakeEvent> parse(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(removeByteOrderMark(rawJson));
            JsonNode event = root.path("event");
            if (!event.isObject()) {
                return Optional.empty();
            }
            String userId = text(root, "userId");
            Instant publishedAt = instant(root.path("publishedAt"));
            Instant eventTimestamp = firstInstant(instant(event.path("timestamp")), publishedAt);
            if (!StringUtils.hasText(userId) || eventTimestamp == null) {
                return Optional.empty();
            }
            return Optional.of(new NormalizedDatalakeEvent(
                    userId,
                    text(root, "providerId"),
                    text(root, "athleteId"),
                    text(root, "eventType"),
                    text(root, "eventName"),
                    text(root, "eventId"),
                    eventTimestamp,
                    publishedAt,
                    event
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Bus event payload is not valid JSON", exception);
        }
    }

    private String text(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText(null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Instant firstInstant(Instant first, Instant second) {
        return first != null ? first : second;
    }

    private Instant instant(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return decimalSecondsToInstant(node.decimalValue());
        }
        String value = node.asText(null);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value.trim()).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    return decimalSecondsToInstant(new BigDecimal(value.trim()));
                } catch (NumberFormatException ignoredNumeric) {
                    return null;
                }
            }
        }
    }

    private Instant decimalSecondsToInstant(BigDecimal seconds) {
        long epochSecond = seconds.longValue();
        int nanos = seconds.subtract(BigDecimal.valueOf(epochSecond))
                .movePointRight(9)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        if (nanos >= 1_000_000_000) {
            epochSecond++;
            nanos -= 1_000_000_000;
        }
        return Instant.ofEpochSecond(epochSecond, nanos);
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
