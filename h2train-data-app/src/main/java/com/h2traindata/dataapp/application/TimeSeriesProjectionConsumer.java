package com.h2traindata.dataapp.application;

import com.h2traindata.bus.BusMessageHandler;
import com.h2traindata.bus.IncomingBusMessage;
import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import com.h2traindata.dataapp.domain.TimeSeriesProjectionResult;
import com.h2traindata.dataapp.infrastructure.DatalakeEventJsonParser;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TimeSeriesProjectionConsumer implements BusMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(TimeSeriesProjectionConsumer.class);

    private final DatalakeEventJsonParser eventJsonParser;
    private final TimeSeriesProjectionService projectionService;

    public TimeSeriesProjectionConsumer(DatalakeEventJsonParser eventJsonParser,
                                        TimeSeriesProjectionService projectionService) {
        this.eventJsonParser = eventJsonParser;
        this.projectionService = projectionService;
    }

    @Override
    public void handle(IncomingBusMessage message) {
        Optional<NormalizedDatalakeEvent> event = eventJsonParser.parse(message.payload());
        if (event.isEmpty()) {
            log.debug("Ignoring bus message without a normalized event source={} channel={} offset={}",
                    message.source(), message.channel(), message.offset());
            return;
        }
        TimeSeriesProjectionResult result = projectionService.process(event.get());
        log.info(
                "Projected time-series event source={} channel={} partition={} offset={} userId={} provider={} eventType={} eventName={} duplicate={} contributions={}",
                message.source(),
                message.channel(),
                message.partition(),
                message.offset(),
                event.get().userId(),
                event.get().providerId(),
                event.get().eventType(),
                event.get().eventName(),
                result.duplicate(),
                result.contributionCount()
        );
    }
}
