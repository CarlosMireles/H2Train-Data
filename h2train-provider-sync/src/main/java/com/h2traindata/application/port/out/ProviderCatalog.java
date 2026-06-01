package com.h2traindata.application.port.out;

import com.h2traindata.domain.EventType;
import java.util.Collection;

public interface ProviderCatalog {

    ProviderConnector connector(String providerId);

    ProviderEventCollector collector(String providerId, EventType eventType);

    Collection<EventType> supportedEventTypes(String providerId);

    Collection<String> registeredProviderIds();
}
