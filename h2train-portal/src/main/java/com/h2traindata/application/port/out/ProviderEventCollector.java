package com.h2traindata.application.port.out;

import com.h2traindata.domain.EventBatch;
import com.h2traindata.domain.EventType;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncCursor;

public interface ProviderEventCollector {

    String providerId();

    EventType eventType();

    EventBatch collect(ProviderConnection connection, SyncCursor cursor);
}
