package com.h2traindata.dataapp.application.port;

import com.h2traindata.bus.IncomingBusMessage;
import com.h2traindata.dataapp.domain.DatalakeReadRequest;
import java.util.stream.Stream;

public interface DatalakeReader {

    Stream<IncomingBusMessage> readEvents(DatalakeReadRequest request);
}
