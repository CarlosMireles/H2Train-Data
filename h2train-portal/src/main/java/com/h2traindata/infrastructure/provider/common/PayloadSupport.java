package com.h2traindata.infrastructure.provider.common;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class PayloadSupport {

    private PayloadSupport() {
    }

    public static void put(Map<String, Object> target, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String string && !StringUtils.hasText(string)) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    public static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value);
        return StringUtils.hasText(string) ? string : null;
    }

    public static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && StringUtils.hasText(string)) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && StringUtils.hasText(string)) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static Double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string && StringUtils.hasText(string)) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value instanceof String string && StringUtils.hasText(string)) {
            return Boolean.parseBoolean(string);
        }
        return null;
    }

    public static Instant instantValue(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        String string = stringValue(value);
        if (string == null) {
            return null;
        }
        try {
            return Instant.parse(string);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(string).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    public static OffsetDateTime offsetDateTimeValue(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        String string = stringValue(value);
        if (string == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(string);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return copy;
        }
        return Map.of();
    }

    public static List<?> listValue(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    public static List<Map<String, Object>> listOfMaps(Object value) {
        return listValue(value).stream()
                .map(PayloadSupport::mapValue)
                .filter(map -> !map.isEmpty())
                .toList();
    }
}
