package com.h2traindata.bus;

import com.h2traindata.domain.EventPublication;
import java.util.List;

public interface EventPublisher {

    void publish(EventPublication publication);

    default void publishAll(List<EventPublication> publications) {
        if (publications == null || publications.isEmpty()) {
            return;
        }
        publications.forEach(this::publish);
    }
}
