package com.h2traindata.datalake.application;

import com.h2traindata.datalake.domain.DatalakeEventRecord;
import com.h2traindata.datalake.domain.IncomingBusMessage;
import com.h2traindata.datalake.io.DatalakeDeadLetterWriter;
import com.h2traindata.datalake.io.DatalakeEventParser;
import com.h2traindata.datalake.io.DatalakeEventWriter;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatalakeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DatalakeIngestionService.class);

    private final DatalakeEventParser eventParser;
    private final DatalakeEventWriter eventWriter;
    private final DatalakeDeadLetterWriter deadLetterWriter;

    public DatalakeIngestionService(DatalakeEventParser eventParser,
                                    DatalakeEventWriter eventWriter,
                                    DatalakeDeadLetterWriter deadLetterWriter) {
        this.eventParser = eventParser;
        this.eventWriter = eventWriter;
        this.deadLetterWriter = deadLetterWriter;
    }

    public DatalakeIngestionResult ingest(IncomingBusMessage message) {
        try {
            DatalakeEventRecord eventRecord = eventParser.parse(message.payload());
            Path target = eventWriter.write(eventRecord);
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
            Path target = deadLetterWriter.write(message, exception);
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
}
