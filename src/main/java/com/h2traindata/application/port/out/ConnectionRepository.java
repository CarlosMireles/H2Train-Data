package com.h2traindata.application.port.out;

import com.h2traindata.domain.ProviderConnection;
import java.util.List;
import java.util.Optional;

public interface ConnectionRepository {

    void save(ProviderConnection connection);

    Optional<ProviderConnection> findByProviderAndAthlete(String providerId, String athleteId);

    List<ProviderConnection> findAll();
}
