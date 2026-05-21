package com.h2traindata.bus;

public interface BusMessageHandler {

    void handle(IncomingBusMessage message);
}
