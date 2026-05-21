package com.h2traindata.infrastructure.provider.common;

import static com.h2traindata.infrastructure.provider.common.PayloadSupport.put;

import com.h2traindata.domain.BaseEvent;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class OntologyEventSupport {

    private OntologyEventSupport() {
    }

    public static Map<String, Object> baseEvent(Instant timestamp, String sourceSystem, String athleteId) {
        return baseEvent(baseEventRecord(timestamp, sourceSystem, athleteId));
    }

    public static BaseEvent baseEventRecord(Instant timestamp, String sourceSystem, String athleteId) {
        return new BaseEvent(timestamp, sourceSystem, athleteId);
    }

    public static Map<String, Object> baseEvent(BaseEvent baseEvent) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putBaseEvent(payload, baseEvent);
        return payload;
    }

    public static void putBaseEvent(Map<String, Object> target,
                                    Instant timestamp,
                                    String sourceSystem,
                                    String athleteId) {
        putBaseEvent(target, new BaseEvent(timestamp, sourceSystem, athleteId));
    }

    public static void putBaseEvent(Map<String, Object> target, BaseEvent baseEvent) {
        put(target, "timestamp", baseEvent.timestamp());
        put(target, "sourceSystem", baseEvent.sourceSystem());
        put(target, "athleteId", baseEvent.athleteId());
    }

    public static String snapshotEventId(String athleteId, String eventName, Instant timestamp) {
        return snapshotEventId(athleteId, eventName, null, timestamp);
    }

    public static String snapshotEventId(String athleteId,
                                         String eventName,
                                         String qualifier,
                                         Instant timestamp) {
        StringBuilder builder = new StringBuilder();
        builder.append(athleteId);
        builder.append(':');
        builder.append(eventName);
        if (StringUtils.hasText(qualifier)) {
            builder.append(':');
            builder.append(qualifier);
        }
        builder.append(':');
        builder.append(timestamp.toEpochMilli());
        return builder.toString();
    }
}
