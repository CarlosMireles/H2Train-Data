package com.h2traindata.application.port.out;

import com.h2traindata.domain.ProviderConnection;
import java.net.URI;

public interface ProviderConnector {

    String providerId();

    URI buildAuthorizationUri();

    default URI buildAuthorizationUri(String state) {
        return buildAuthorizationUri();
    }

    ProviderConnection connect(String code);

    default ProviderConnection refresh(ProviderConnection connection) {
        return connection;
    }
}
