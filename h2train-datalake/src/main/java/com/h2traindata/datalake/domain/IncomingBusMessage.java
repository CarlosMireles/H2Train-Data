package com.h2traindata.datalake.domain;

public record IncomingBusMessage(
        String source,
        String channel,
        Integer partition,
        Long offset,
        String key,
        String payload
) {
}
