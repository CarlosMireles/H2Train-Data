package com.h2traindata.infrastructure.datalake;

import com.h2traindata.domain.EventType;
import org.springframework.stereotype.Component;

@Component
public class DatalakeResourceResolver {

    public String resolve(EventType eventType) {
        return switch (eventType) {
            case ACTIVITY -> "activity";
            case USER_METRICS -> "user_metrics";
        };
    }
}
