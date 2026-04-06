package com.h2traindata.application.usecase;

import com.h2traindata.application.service.ProviderRegistry;
import java.net.URI;
import org.springframework.stereotype.Service;

@Service
public class StartAuthorizationUseCase {

    private final ProviderRegistry providerRegistry;

    public StartAuthorizationUseCase(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public URI execute(String providerId) {
        return providerRegistry.connector(providerId).buildAuthorizationUri();
    }
}
