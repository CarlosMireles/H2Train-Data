package com.h2traindata.web.identity;

public interface ExternalIdentityProviderCatalog {

    ExternalIdentityProvider provider(String providerId);

    boolean isConfigured(String providerId);
}
