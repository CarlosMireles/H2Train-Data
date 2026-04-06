package com.h2traindata.application.usecase;

import com.h2traindata.application.port.out.ConnectionRepository;
import com.h2traindata.application.service.ProviderRegistry;
import com.h2traindata.domain.ProviderConnection;
import org.springframework.stereotype.Service;

@Service
public class HandleAuthorizationCallbackUseCase {

    private final ProviderRegistry providerRegistry;
    private final ConnectionRepository connectionRepository;

    public HandleAuthorizationCallbackUseCase(ProviderRegistry providerRegistry,
                                              ConnectionRepository connectionRepository) {
        this.providerRegistry = providerRegistry;
        this.connectionRepository = connectionRepository;
    }

    public ProviderConnection execute(String providerId, String code) {
        ProviderConnection connection = providerRegistry.connector(providerId).connect(code);
        connectionRepository.save(connection);
        return connection;
    }
}
