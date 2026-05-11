package com.h2traindata.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.h2traindata.infrastructure.provider.common.PayloadSupport;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProviderEvent(
        @JsonIgnore String providerId,
        String athleteId,
        EventType eventType,
        String eventName,
        String eventId,
        Instant timestamp,
        String sourceSystem,
        @JsonIgnore Map<String, Object> attributes
) {
    public ProviderEvent {
        attributes = immutableMap(attributes);
    }

    public ProviderEvent(String providerId,
                         String athleteId,
                         EventType eventType,
                         String eventName,
                         String eventId,
                         Instant occurredAt,
                         Map<String, Object> eventFields,
                         Map<String, Object> ignoredProviderSpecificPayload,
                         Map<String, Object> ignoredRawPayload) {
        this(providerId, athleteId, eventType, eventName, eventId, occurredAt, eventFields);
    }

    public ProviderEvent(String providerId,
                         String athleteId,
                         EventType eventType,
                         String eventName,
                         String eventId,
                         Instant occurredAt,
                         Map<String, Object> eventFields) {
        this(
                providerId,
                resolveAthleteId(athleteId, eventFields),
                eventType,
                eventName,
                eventId,
                resolveTs(occurredAt, eventFields),
                resolveSourceSystem(providerId, eventFields),
                extractAttributes(eventFields)
        );
    }

    public Instant occurredAt() {
        return timestamp;
    }

    @JsonAnyGetter
    public Map<String, Object> fields() {
        return attributes;
    }

    public Object field(String key) {
        return attributes.get(key);
    }

    private static String resolveAthleteId(String athleteId, Map<String, Object> eventFields) {
        String fromPayload = PayloadSupport.stringValue(rawBaseField(eventFields, "athleteId", "id"));
        return fromPayload != null ? fromPayload : athleteId;
    }

    private static Instant resolveTs(Instant occurredAt, Map<String, Object> eventFields) {
        Instant fromPayload = PayloadSupport.instantValue(rawBaseField(eventFields, "timestamp", "ts"));
        return fromPayload != null ? fromPayload : occurredAt;
    }

    private static String resolveSourceSystem(String providerId, Map<String, Object> eventFields) {
        String fromPayload = PayloadSupport.stringValue(rawBaseField(eventFields, "sourceSystem", "ss"));
        return fromPayload != null ? fromPayload : providerId;
    }

    private static Map<String, Object> extractAttributes(Map<String, Object> eventFields) {
        if (eventFields == null || eventFields.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> attributes = new LinkedHashMap<>(eventFields);
        attributes.remove("timestamp");
        attributes.remove("sourceSystem");
        attributes.remove("ts");
        attributes.remove("ss");
        attributes.remove("athleteId");
        attributes.remove("id");
        return attributes;
    }

    private static Object rawBaseField(Map<String, Object> eventFields, String... keys) {
        if (eventFields == null || eventFields.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            Object value = eventFields.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Object immutableValue(Object source) {
        if (source instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), immutableValue(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }
        if (source instanceof List<?> list) {
            return Collections.unmodifiableList(list.stream()
                    .map(ProviderEvent::immutableValue)
                    .toList());
        }
        return source;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return (Map<String, Object>) immutableValue(source);
    }
}
