package com.h2traindata.application.port.out;

import com.h2traindata.domain.EventBatch;

public interface EventSink {

    void write(EventBatch batch);
}
