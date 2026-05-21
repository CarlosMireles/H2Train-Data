package com.h2traindata.bus;

public record IncomingBusMessage(
        String source,
        String channel,
        Integer partition,
        Long offset,
        String key,
        String payload
) {
}
