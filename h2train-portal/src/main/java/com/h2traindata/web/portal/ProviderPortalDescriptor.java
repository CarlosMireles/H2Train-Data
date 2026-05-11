package com.h2traindata.web.portal;

import java.util.Locale;

public record ProviderPortalDescriptor(
        String providerId,
        String displayName,
        String description,
        String themeStyle,
        String logoMarkup
) {

    public String domProviderId() {
        return providerId.toLowerCase(Locale.ROOT);
    }
}
