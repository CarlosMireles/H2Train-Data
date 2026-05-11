package com.h2traindata.web.portal;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ProviderPortalDescriptorFactory {

    public ProviderPortalDescriptor create(String providerId) {
        return switch (providerId.toLowerCase(Locale.ROOT)) {
            case "fitbit" -> new ProviderPortalDescriptor(
                    providerId,
                    "Fitbit",
                    "Sync Fitbit activities and user metric snapshots, then configure whether H2Train should collect updates every 5 hours, every 24 hours, or every 7 days.",
                    "--provider-primary:#00b0b9;--provider-deep:#007e86;--provider-soft:rgba(0,176,185,0.14);",
                    """
                    <svg width="34" height="34" viewBox="0 0 34 34" aria-label="Fitbit logo" role="img">
                        <circle cx="9" cy="8" r="2" fill="#ffffff"/>
                        <circle cx="17" cy="8" r="2" fill="#ffffff"/>
                        <circle cx="25" cy="8" r="2" fill="#ffffff"/>
                        <circle cx="9" cy="17" r="2" fill="#ffffff"/>
                        <circle cx="17" cy="17" r="2.4" fill="#ffffff"/>
                        <circle cx="25" cy="17" r="2" fill="#ffffff"/>
                        <circle cx="9" cy="26" r="2" fill="#ffffff"/>
                        <circle cx="17" cy="26" r="2" fill="#ffffff"/>
                    </svg>
                    """
            );
            case "strava" -> new ProviderPortalDescriptor(
                    providerId,
                    "Strava",
                    "Import Strava activities and athlete metric snapshots, remember the authorization state, and control the automatic sync cadence from the portal.",
                    "--provider-primary:#fc4c02;--provider-deep:#c53c02;--provider-soft:rgba(252,76,2,0.14);",
                    """
                    <svg width="32" height="32" viewBox="0 0 32 32" aria-label="Strava logo" role="img">
                        <path fill="#ffffff" d="M12 3 19.2 17H14.8L12 11.5 9.2 17H4.8z"/>
                        <path fill="#ffffff" d="M21 14.5 27.2 26H23.4L21 21.4 18.6 26H14.8z"/>
                    </svg>
                    """
            );
            default -> new ProviderPortalDescriptor(
                    providerId,
                    Character.toUpperCase(providerId.charAt(0)) + providerId.substring(1),
                    "Sync provider events and send them to the analytics pipeline.",
                    "--provider-primary:#0b57d0;--provider-deep:#08307b;--provider-soft:rgba(11,87,208,0.12);",
                    """
                    <svg width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
                        <circle cx="16" cy="16" r="10" fill="#ffffff"/>
                    </svg>
                    """
            );
        };
    }
}
