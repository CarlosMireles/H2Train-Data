package com.h2traindata.web;

import com.h2traindata.web.identity.ExternalIdentityProviderCatalog;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginAvailability {

    private static final String GOOGLE_PROVIDER_ID = "google";

    private final ExternalIdentityProviderCatalog identityProviderCatalog;

    public GoogleLoginAvailability(ExternalIdentityProviderCatalog identityProviderCatalog) {
        this.identityProviderCatalog = identityProviderCatalog;
    }

    public boolean isAvailable() {
        return identityProviderCatalog.isConfigured(GOOGLE_PROVIDER_ID);
    }
}
