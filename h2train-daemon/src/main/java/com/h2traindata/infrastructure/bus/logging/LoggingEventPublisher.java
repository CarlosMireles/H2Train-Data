package com.h2traindata.infrastructure.bus.logging;

import com.h2traindata.application.port.out.EventPublisher;
import com.h2traindata.domain.ProviderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(ProviderEvent event) {
        log.info(
                "Published event provider={} athleteId={} eventType={} eventName={} eventId={} timestamp={}",
                event.providerId(),
                event.athleteId(),
                event.eventType(),
                event.eventName(),
                event.eventId(),
                event.timestamp()
        );
    }
}
