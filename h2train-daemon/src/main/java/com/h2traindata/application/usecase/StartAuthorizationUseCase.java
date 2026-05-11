package com.h2traindata.application.usecase;

import com.h2traindata.application.service.ProviderRegistry;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StartAuthorizationUseCase {

    private final ProviderRegistry providerRegistry;

    public StartAuthorizationUseCase(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    public URI execute(String providerId, String userId) {
        if (!StringUtils.hasText(userId)) {
            return providerRegistry.connector(providerId).buildAuthorizationUri();
        }
        return providerRegistry.connector(providerId).buildAuthorizationUri(userId);
    }
}
