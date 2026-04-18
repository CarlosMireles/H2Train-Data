package com.h2traindata.infrastructure.provider.fitbit;

import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.domain.SyncCursor;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.fitbit", name = "enabled", havingValue = "true")
public class FitbitUserMetricsCollector implements ProviderEventCollector {

    private final FitbitUserMetricsPayloadFetcher payloadFetcher;
    private final FitbitUserMetricsEventFactory eventFactory;

    public FitbitUserMetricsCollector(FitbitUserMetricsPayloadFetcher payloadFetcher,
                                      FitbitUserMetricsEventFactory eventFactory) {
        this.payloadFetcher = payloadFetcher;
        this.eventFactory = eventFactory;
    }

    @Override
    public String providerId() {
        return "fitbit";
    }

    @Override
    public EventType eventType() {
        return EventType.USER_METRICS;
    }

    @Override
    public EventBatch collect(ProviderConnection connection, SyncCursor cursor) {
        Instant snapshotAt = Instant.now();
        FitbitUserMetricsPayloadBundle payloadBundle = payloadFetcher.fetch(connection);
        ProviderEvent event = eventFactory.createEvent(
                providerId(),
                connection.athlete().id(),
                eventType(),
                snapshotAt,
                payloadBundle
        );

        return new EventBatch(
                providerId(),
                connection.athlete().id(),
                eventType(),
                java.util.List.of(event),
                null
        );
    }
}
