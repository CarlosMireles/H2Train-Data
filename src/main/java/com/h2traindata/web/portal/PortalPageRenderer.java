package com.h2traindata.web.portal;

import com.h2traindata.domain.SyncInterval;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PortalPageRenderer {

    private final ProviderPortalDescriptorFactory descriptorFactory;

    public PortalPageRenderer(ProviderPortalDescriptorFactory descriptorFactory) {
        this.descriptorFactory = descriptorFactory;
    }

    public String render(Collection<String> providerIds) {
        String providerCards = providerIds.stream()
                .sorted()
                .map(descriptorFactory::create)
                .map(this::renderProviderCard)
                .collect(Collectors.joining());

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>H2Train Data</title>
                    <link rel="stylesheet" href="/portal.css">
                </head>
                <body>
                    <div class="shell">
                        <section class="hero">
                            <div class="brand-lockup">
                                <img class="brand-logo" src="/h2train-logo.png" alt="H2Train logo">
                                <div class="eyebrow">Data Ingestion Portal</div>
                            </div>
                            <h1>H2Train Data</h1>
                            <p class="hero-copy">
                                Connect your sports providers, keep their consent status visible, and control how often
                                H2Train pulls new activity and user metric data after the initial authorization.
                            </p>
                        </section>

                        <section>
                            <div class="grid-header">
                                <h2>Available Providers</h2>
                            </div>
                            <div class="providers">
                                __PROVIDER_CARDS__
                            </div>
                        </section>
                    </div>
                    <script src="/portal.js" defer></script>
                </body>
                </html>
                """.replace("__PROVIDER_CARDS__", providerCards);
    }

    private String renderProviderCard(ProviderPortalDescriptor descriptor) {
        return """
                <article class="provider-card" style="%s" data-provider-id="%s">
                    <div class="provider-tag">Provider Sync</div>
                    <div class="provider-top">
                        <div class="provider-head">
                            <div class="provider-logo">
                                %s
                            </div>
                            <div class="provider-meta">
                                <h3>%s</h3>
                                <span>OAuth connection and event ingestion</span>
                            </div>
                        </div>
                        <a class="connect-link" href="/auth/%s/login">Connect</a>
                    </div>
                    <p>%s</p>
                    <div class="sync-panel">
                        <div class="sync-summary">
                            <div class="sync-summary-row">
                                <span class="sync-summary-label">Athlete</span>
                                <strong class="sync-summary-value" data-role="athlete">Waiting for authorization</strong>
                            </div>
                            <div class="sync-summary-row">
                                <span class="sync-summary-label">Consent</span>
                                <strong class="sync-summary-value" data-role="consent">Pending</strong>
                            </div>
                            <div class="sync-summary-row">
                                <span class="sync-summary-label">Last sync</span>
                                <strong class="sync-summary-value" data-role="last-sync">Not synced yet</strong>
                            </div>
                        </div>
                        <div class="sync-controls">
                            <div class="sync-controls-header">
                                <div class="toggle-label">
                                    <strong>Automatic sync</strong>
                                    <span>Enable or pause scheduled collection</span>
                                </div>
                                <button class="sync-toggle" type="button" role="switch" aria-checked="false" disabled>Sync off</button>
                            </div>
                            <label class="interval-control">
                                <span>Collection interval</span>
                                <select class="sync-interval" disabled>
                %s
                                </select>
                            </label>
                            <p class="provider-status">
                                Authorize the provider to remember consent and enable automatic sync controls.
                            </p>
                        </div>
                    </div>
                </article>
                """.formatted(
                descriptor.themeStyle(),
                descriptor.domProviderId(),
                descriptor.logoMarkup(),
                descriptor.displayName(),
                descriptor.providerId(),
                descriptor.description(),
                renderIntervalOptions()
        );
    }

    private String renderIntervalOptions() {
        return java.util.Arrays.stream(SyncInterval.values())
                .map(interval -> "                                    <option value=\""
                        + interval.name()
                        + "\">"
                        + interval.label()
                        + "</option>")
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
