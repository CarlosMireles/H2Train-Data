package com.h2traindata.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProviderEvent(
        String providerId,
        String athleteId,
        EventType eventType,
        String eventId,
        Instant occurredAt,
        Map<String, Object> normalizedPayload,
        Map<String, Object> providerSpecificPayload,
        Map<String, Object> rawPayload
) {
    public ProviderEvent {
        normalizedPayload = immutableMap(normalizedPayload);
        providerSpecificPayload = immutableMap(providerSpecificPayload);
        rawPayload = immutableMap(rawPayload);
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
