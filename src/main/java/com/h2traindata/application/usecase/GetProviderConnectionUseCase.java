package com.h2traindata.application.usecase;

import com.h2traindata.application.exception.ConnectionNotFoundException;
import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.domain.ProviderConnection;
import org.springframework.stereotype.Service;

@Service
public class GetProviderConnectionUseCase {

    private final ConnectionRepository connectionRepository;

    public GetProviderConnectionUseCase(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    public ProviderConnection execute(String providerId, String athleteId) {
        return connectionRepository.findByProviderAndAthlete(providerId, athleteId)
                .orElseThrow(() -> new ConnectionNotFoundException(providerId, athleteId));
    }
}
