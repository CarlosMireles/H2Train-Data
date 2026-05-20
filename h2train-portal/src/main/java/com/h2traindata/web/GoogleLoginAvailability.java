package com.h2traindata.web;

import com.h2traindata.web.google.GoogleAuthProperties;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginAvailability {

    private final GoogleAuthProperties properties;

    public GoogleLoginAvailability(GoogleAuthProperties properties) {
        this.properties = properties;
    }

    public boolean isAvailable() {
        return properties.isConfigured();
    }
}
