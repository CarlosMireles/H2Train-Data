package com.h2traindata.infrastructure.bus.logging;

import com.h2traindata.application.port.out.EventPublisher;
import com.h2traindata.domain.EventPublication;
import com.h2traindata.domain.ProviderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.bus", name = "type", havingValue = "logging", matchIfMissing = true)
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(EventPublication publication) {
        ProviderEvent event = publication.event();
        log.info(
                "Published event userId={} provider={} athleteId={} eventType={} eventName={} eventId={} timestamp={}",
                publication.userId(),
                event.providerId(),
                event.athleteId(),
                event.eventType(),
                event.eventName(),
                event.eventId(),
                event.timestamp()
        );
    }
}
