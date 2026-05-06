package com.h2traindata.infrastructure.datalake;

import com.h2traindata.application.port.out.EventSink;
import com.h2traindata.domain.EventBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEventSink implements EventSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventSink.class);

    @Override
    public void write(EventBatch batch) {
        log.info(
                "Persisting {} {} events for athlete {} from provider {} to the configured event sink",
                batch.events().size(),
                batch.eventType(),
                batch.athleteId(),
                batch.providerId()
        );
    }
}
