package com.h2traindata.infrastructure.persistence;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.domain.ProviderConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryConnectionRepository implements ConnectionRepository {

    private final Map<String, Map<String, ProviderConnection>> connections = new ConcurrentHashMap<>();

    @Override
    public void save(ProviderConnection connection) {
        connections.computeIfAbsent(connection.providerId(), ignored -> new ConcurrentHashMap<>())
                .put(connection.athlete().id(), connection);
    }

    @Override
    public Optional<ProviderConnection> findByProviderAndAthlete(String providerId, String athleteId) {
        return Optional.ofNullable(connections.getOrDefault(providerId, Map.of()).get(athleteId));
    }

    @Override
    public List<ProviderConnection> findAll() {
        return connections.values().stream()
                .flatMap(providerConnections -> providerConnections.values().stream())
                .toList();
    }
}
