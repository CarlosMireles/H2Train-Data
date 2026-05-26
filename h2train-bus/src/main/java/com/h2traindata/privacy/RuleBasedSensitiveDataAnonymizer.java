package com.h2traindata.privacy;

import com.h2traindata.domain.BusEventEnvelope;
import com.h2traindata.domain.EventPublication;
import com.h2traindata.domain.ProviderEvent;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedSensitiveDataAnonymizer implements SensitiveDataAnonymizer {

    public static final String ANONYMIZED_VALUE = "[ANONYMIZED]";
    public static final String REDACTED_VALUE = "[REDACTED]";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "email",
            "mail",
            "username",
            "user",
            "userlogin",
            "displayname",
            "fullname",
            "name",
            "firstname",
            "lastname",
            "givenname",
            "familyname",
            "providerathleteid",
            "providerathleteusername",
            "athleteusername"
    );
    private static final Set<String> SECRET_KEYS = Set.of(
            "password",
            "passwordhash",
            "accesstoken",
            "refreshtoken",
            "token",
            "secret"
    );

    @Override
    public Map<String, Object> anonymizeFields(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> anonymized = new LinkedHashMap<>();
        fields.forEach((key, value) -> anonymized.put(key, anonymizeValue(key, value)));
        return Collections.unmodifiableMap(anonymized);
    }

    @Override
    public ProviderEvent anonymizeEvent(ProviderEvent event) {
        if (event == null) {
            return null;
        }
        return new ProviderEvent(
                event.providerId(),
                event.athleteId(),
                event.eventType(),
                event.eventName(),
                event.eventId(),
                event.timestamp(),
                event.sourceSystem(),
                anonymizeFields(event.fields())
        );
    }

    @Override
    public EventPublication anonymizePublication(EventPublication publication) {
        if (publication == null) {
            return null;
        }
        return new EventPublication(publication.userId(), anonymizeEvent(publication.event()));
    }

    @Override
    public BusEventEnvelope anonymizeEnvelope(BusEventEnvelope envelope) {
        if (envelope == null) {
            return null;
        }
        return new BusEventEnvelope(
                envelope.messageId(),
                envelope.schemaVersion(),
                envelope.publishedAt(),
                envelope.userId(),
                envelope.providerId(),
                envelope.athleteId(),
                envelope.eventType(),
                envelope.eventName(),
                envelope.eventId(),
                anonymizeEvent(envelope.event())
        );
    }

    private Object anonymizeValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        String normalizedKey = normalizeKey(key);
        if (SECRET_KEYS.contains(normalizedKey)) {
            return REDACTED_VALUE;
        }
        if (SENSITIVE_KEYS.contains(normalizedKey)) {
            return ANONYMIZED_VALUE;
        }
        if (value instanceof Map<?, ?> map) {
            return anonymizeMap(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> anonymizeValue(key, item))
                    .toList();
        }
        if (value instanceof String string && EMAIL_PATTERN.matcher(string).find()) {
            return ANONYMIZED_VALUE;
        }
        return value;
    }

    private Map<String, Object> anonymizeMap(Map<?, ?> source) {
        Map<String, Object> anonymized = new LinkedHashMap<>();
        source.forEach((key, value) -> anonymized.put(String.valueOf(key), anonymizeValue(String.valueOf(key), value)));
        return Collections.unmodifiableMap(anonymized);
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
