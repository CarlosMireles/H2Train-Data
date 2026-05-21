package com.h2traindata.dataapp.application.port;

import com.h2traindata.bus.IncomingBusMessage;
import com.h2traindata.domain.BusEventEnvelope;

public interface EventEnvelopeParser {

    BusEventEnvelope parse(IncomingBusMessage message);
}
