package com.h2traindata.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record EventBatch(
        String providerId,
        String athleteId,
        EventType eventType,
        List<ProviderEvent> events,
        SyncCursor nextCursor
) {
    public EventBatch {
        events = events == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(events));
    }
}
