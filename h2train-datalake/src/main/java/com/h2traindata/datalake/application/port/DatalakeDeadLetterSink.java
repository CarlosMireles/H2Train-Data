package com.h2traindata.datalake.application.port;

import com.h2traindata.bus.IncomingBusMessage;
import java.nio.file.Path;

public interface DatalakeDeadLetterSink {

    Path write(IncomingBusMessage message, RuntimeException exception);
}
