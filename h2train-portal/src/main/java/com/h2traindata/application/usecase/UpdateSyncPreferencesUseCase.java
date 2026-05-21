package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.ConnectionNotFoundException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncPreferences;
import org.springframework.stereotype.Service;

@Service
public class UpdateSyncPreferencesUseCase {

    private final ConnectionRepository connectionRepository;

    public UpdateSyncPreferencesUseCase(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    public ProviderConnection execute(String providerId, String athleteId, SyncPreferences syncPreferences) {
        ProviderConnection connection = connectionRepository.findByProviderAndAthlete(providerId, athleteId)
                .orElseThrow(() -> new ConnectionNotFoundException(providerId, athleteId));

        ProviderConnection updatedConnection = connection.withSyncPreferences(syncPreferences);
        connectionRepository.save(updatedConnection);
        return updatedConnection;
    }
}
