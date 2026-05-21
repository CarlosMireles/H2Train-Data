package com.h2traindata.web.identity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ExternalIdentityProviderRegistry implements ExternalIdentityProviderCatalog {

    private final Map<String, ExternalIdentityProvider> providers;

    public ExternalIdentityProviderRegistry(List<ExternalIdentityProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(ExternalIdentityProvider::providerId, Function.identity()));
    }

    @Override
    public ExternalIdentityProvider provider(String providerId) {
        ExternalIdentityProvider provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown external identity provider: " + providerId);
        }
        return provider;
    }

    @Override
    public boolean isConfigured(String providerId) {
        ExternalIdentityProvider provider = providers.get(providerId);
        return provider != null && provider.isConfigured();
    }
}
