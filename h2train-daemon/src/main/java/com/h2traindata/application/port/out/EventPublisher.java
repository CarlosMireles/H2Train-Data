package com.h2traindata.application.port.out;

import com.h2traindata.domain.ProviderEvent;
import java.util.List;

public interface EventPublisher {

    void publish(ProviderEvent event);

    default void publishAll(List<ProviderEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        events.forEach(this::publish);
    }
}
