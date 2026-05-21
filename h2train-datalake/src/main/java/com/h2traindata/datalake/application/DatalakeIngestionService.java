package com.h2traindata.datalake.application;

import com.h2traindata.bus.BusMessageHandler;
import com.h2traindata.bus.IncomingBusMessage;
import com.h2traindata.datalake.application.port.DatalakeDeadLetterSink;
import com.h2traindata.datalake.application.port.DatalakeEventSink;
import com.h2traindata.datalake.domain.DatalakeEventRecord;
import com.h2traindata.datalake.io.DatalakeEventParser;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatalakeIngestionService implements BusMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(DatalakeIngestionService.class);

    private final DatalakeEventParser eventParser;
    private final DatalakeEventSink eventSink;
    private final DatalakeDeadLetterSink deadLetterSink;

    public DatalakeIngestionService(DatalakeEventParser eventParser,
                                    DatalakeEventSink eventSink,
                                    DatalakeDeadLetterSink deadLetterSink) {
        this.eventParser = eventParser;
        this.eventSink = eventSink;
        this.deadLetterSink = deadLetterSink;
    }

    public DatalakeIngestionResult ingest(IncomingBusMessage message) {
        try {
            DatalakeEventRecord eventRecord = eventParser.parse(message.payload());
            Path target = eventSink.write(eventRecord);
            log.info(
                    "Wrote bus event to datalake source={} channel={} partition={} offset={} userId={} provider={} eventType={} file={}",
                    message.source(),
                    message.channel(),
                    message.partition(),
                    message.offset(),
                    eventRecord.userId(),
                    eventRecord.providerId(),
                    eventRecord.eventType(),
                    target
            );
            return DatalakeIngestionResult.success(target, eventRecord);
        } catch (RuntimeException exception) {
            Path target = deadLetterSink.write(message, exception);
            log.warn(
                    "Wrote bus event to datalake dead-letter source={} channel={} partition={} offset={} reason={} file={}",
                    message.source(),
                    message.channel(),
                    message.partition(),
                    message.offset(),
                    exception.getMessage(),
                    target
            );
            log.debug("Dead-letter payload details", exception);
            return DatalakeIngestionResult.failure(target, exception);
        }
    }

    @Override
    public void handle(IncomingBusMessage message) {
        ingest(message);
    }
}
