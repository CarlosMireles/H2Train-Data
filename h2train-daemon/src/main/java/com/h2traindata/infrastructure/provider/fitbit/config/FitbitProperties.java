package com.h2traindata.infrastructure.provider.fitbit.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.fitbit")
public class FitbitProperties {

    private static final String LEGACY_BODY_SCOPE = "body";
    private static final String WEIGHT_SCOPE = "weight";

    private boolean enabled;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private int initialActivityFetchLimit = 10;
    private int incrementalActivityFetchLimit = 20;
    private List<String> scopes = normalizeScopes(List.of(
            "activity",
            "heartrate",
            "location",
            "profile",
            "weight",
            "nutrition",
            "settings",
            "social",
            "sleep",
            "cardio_fitness",
            "electrocardiogram",
            "oxygen_saturation",
            "respiratory_rate",
            "temperature",
            "irregular_rhythm_notifications"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public int getInitialActivityFetchLimit() {
        return initialActivityFetchLimit;
    }

    public void setInitialActivityFetchLimit(int initialActivityFetchLimit) {
        this.initialActivityFetchLimit = Math.max(1, initialActivityFetchLimit);
    }

    public int getIncrementalActivityFetchLimit() {
        return incrementalActivityFetchLimit;
    }

    public void setIncrementalActivityFetchLimit(int incrementalActivityFetchLimit) {
        this.incrementalActivityFetchLimit = Math.max(1, incrementalActivityFetchLimit);
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = normalizeScopes(scopes);
    }

    private static List<String> normalizeScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String scope : scopes) {
            if (!StringUtils.hasText(scope)) {
                continue;
            }
            if (LEGACY_BODY_SCOPE.equals(scope)) {
                normalized.add(WEIGHT_SCOPE);
                continue;
            }
            normalized.add(scope);
        }
        return List.copyOf(new ArrayList<>(normalized));
    }
}
