package com.h2traindata.application.usecase;

import com.h2traindata.application.port.out.ProviderCatalog;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StartAuthorizationUseCase {

    private final ProviderCatalog providerCatalog;

    public StartAuthorizationUseCase(ProviderCatalog providerCatalog) {
        this.providerCatalog = providerCatalog;
    }

    public URI execute(String providerId, String userId) {
        if (!StringUtils.hasText(userId)) {
            return providerCatalog.connector(providerId).buildAuthorizationUri();
        }
        return providerCatalog.connector(providerId).buildAuthorizationUri(userId);
    }
}
