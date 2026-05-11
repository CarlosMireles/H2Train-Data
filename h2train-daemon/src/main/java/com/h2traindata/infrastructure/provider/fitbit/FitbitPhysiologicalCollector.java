package com.h2traindata.infrastructure.provider.fitbit;

import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.ProviderEvent;
import com.h2traindata.domain.SyncCursor;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.fitbit", name = "enabled", havingValue = "true")
public class FitbitPhysiologicalCollector implements ProviderEventCollector {

    private final FitbitUserMetricsPayloadFetcher payloadFetcher;
    private final FitbitUserMetricsEventFactory eventFactory;

    public FitbitPhysiologicalCollector(FitbitUserMetricsPayloadFetcher payloadFetcher,
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
        return EventType.PHYSIOLOGICAL;
    }

    @Override
    public EventBatch collect(ProviderConnection connection, SyncCursor cursor) {
        Instant snapshotAt = Instant.now();
        FitbitUserMetricsPayloadBundle payloadBundle = payloadFetcher.fetch(connection);
        List<ProviderEvent> events = eventFactory.createPhysiologicalEvents(
                providerId(),
                connection.athlete().id(),
                snapshotAt,
                payloadBundle
        );

        return new EventBatch(
                providerId(),
                connection.athlete().id(),
                eventType(),
                events,
                null
        );
    }
}
