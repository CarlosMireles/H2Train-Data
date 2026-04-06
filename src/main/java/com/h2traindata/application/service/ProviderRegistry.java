package com.h2traindata.application.service;

import com.h2traindata.application.exception.UnknownProviderException;
import com.h2traindata.application.exception.UnsupportedEventTypeException;
import com.h2traindata.application.port.out.ProviderConnector;
import com.h2traindata.application.port.out.ProviderEventCollector;
import com.h2traindata.domain.EventType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class ProviderRegistry {

    private final Map<String, ProviderConnector> connectors;
    private final Map<CollectorKey, ProviderEventCollector> collectors;

    public ProviderRegistry(List<ProviderConnector> connectors, List<ProviderEventCollector> collectors) {
        this.connectors = connectors.stream()
                .collect(Collectors.toUnmodifiableMap(ProviderConnector::providerId, Function.identity()));
        this.collectors = collectors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        collector -> new CollectorKey(collector.providerId(), collector.eventType()),
                        Function.identity()
                ));
    }

    public ProviderConnector connector(String providerId) {
        ProviderConnector connector = connectors.get(providerId);
        if (connector == null) {
            throw new UnknownProviderException(providerId);
        }
        return connector;
    }

    public ProviderEventCollector collector(String providerId, EventType eventType) {
        ProviderEventCollector collector = collectors.get(new CollectorKey(providerId, eventType));
        if (collector == null) {
            throw new UnsupportedEventTypeException(providerId, eventType);
        }
        return collector;
    }

    public Collection<String> registeredProviderIds() {
        return Stream.concat(connectors.keySet().stream(), collectors.keySet().stream().map(CollectorKey::providerId))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
    }

    private record CollectorKey(String providerId, EventType eventType) {
    }
}
