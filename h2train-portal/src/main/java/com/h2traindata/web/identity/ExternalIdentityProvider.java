package com.h2traindata.web.identity;

import java.net.URI;

public interface ExternalIdentityProvider {

    String providerId();

    boolean isConfigured();

    URI authorizationUri(String state);

    ExternalIdentityProfile fetchProfile(String code);
}
