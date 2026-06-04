package com.h2traindata.web.portal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.SyncInterval;
import com.h2traindata.web.dto.SyncSettingsResponse;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PortalPageRenderer {

    private final ProviderPortalDescriptorFactory descriptorFactory;
    private final ObjectMapper objectMapper;

    public PortalPageRenderer(ProviderPortalDescriptorFactory descriptorFactory,
                              ObjectMapper objectMapper) {
        this.descriptorFactory = descriptorFactory;
        this.objectMapper = objectMapper;
    }

    public String render(Collection<String> providerIds) {
        return render(providerIds, null, List.of());
    }

    public String render(Collection<String> providerIds,
                         InternalUserAccount userAccount,
                         Collection<SyncSettingsResponse> connections) {
        return render(providerIds, userAccount, connections, null);
    }

    public String render(Collection<String> providerIds,
                         InternalUserAccount userAccount,
                         Collection<SyncSettingsResponse> connections,
                         PortalAlert alert) {
        String providerCards = providerIds.stream()
                .sorted()
                .map(descriptorFactory::create)
                .map(this::renderProviderCard)
                .collect(Collectors.joining());
        String accountEmail = userAccount != null ? userAccount.email() : "";
        String accountUsername = userAccount != null ? userAccount.username() : "";
        String accountId = userAccount != null ? userAccount.id() : "";

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
                        <header class="account-bar">
                            <div>
                                <span class="account-label">Internal account</span>
                                <strong>%s</strong>
                                <span>%s</span>
                            </div>
                            <form method="post" action="/account/logout">
                                <button class="secondary-action" type="submit">Sign out</button>
                            </form>
                        </header>
                        __PORTAL_ALERT__
                        <section class="hero">
                            <div class="brand-lockup">
                                <img class="brand-logo" src="/h2train-logo.png" alt="H2Train logo">
                                <div class="eyebrow">Data Ingestion Portal</div>
                            </div>
                            <h1>H2Train Data</h1>
                            <p class="hero-copy">
                                Connect your sports providers, link them to the same internal H2Train account, keep
                                their consent status visible, and control how often H2Train pulls new state, activity,
                                physiology, body composition, and health events after the initial authorization.
                            </p>
                        </section>

                        <section>
                            <div class="grid-header">
                                <h2>Available Providers</h2>
                                <span class="account-id">User ID %s</span>
                            </div>
                            <div class="providers">
                                __PROVIDER_CARDS__
                            </div>
                        </section>
                    </div>
                    <script>
                        window.H2TRAIN_BOOTSTRAP = __BOOTSTRAP__;
                    </script>
                    <script src="/portal.js" defer></script>
                </body>
                </html>
                """.formatted(
                escape(accountUsername),
                escape(accountEmail),
                escape(accountId)
        )
                .replace("__PROVIDER_CARDS__", providerCards)
                .replace("__PORTAL_ALERT__", renderAlert(alert))
                .replace("__BOOTSTRAP__", bootstrapJson(userAccount, connections));
    }

    private String renderAlert(PortalAlert alert) {
        if (alert == null) {
            return "";
        }

        String actionMarkup = alert.actionHref() == null || alert.actionHref().isBlank()
                ? ""
                : """
                                <a class="portal-alert-action" href="%s">%s</a>
                        """.formatted(escape(alert.actionHref()), escape(alert.actionText()));
        String detailMarkup = alert.detail() == null || alert.detail().isBlank()
                ? ""
                : """
                            <p class="portal-alert-detail">%s</p>
                        """.formatted(escape(alert.detail()));

        return """
                        <section class="portal-alert" data-tone="%s" role="alert">
                            <div class="portal-alert-icon" aria-hidden="true">!</div>
                            <div class="portal-alert-body">
                                <strong>%s</strong>
                                <p>%s</p>
                                %s
                            </div>
                            %s
                        </section>
                """.formatted(
                escape(alert.tone()),
                escape(alert.title()),
                escape(alert.message()),
                detailMarkup,
                actionMarkup
        );
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

    private String bootstrapJson(InternalUserAccount userAccount, Collection<SyncSettingsResponse> connections) {
        try {
            PortalBootstrap bootstrap = new PortalBootstrap(
                    userAccount == null
                            ? null
                            : new AccountBootstrap(
                                    userAccount.id(),
                                    userAccount.email(),
                                    userAccount.username(),
                                    userAccount.providerIds()
                            ),
                    connections
            );
            return objectMapper.writeValueAsString(bootstrap).replace("</", "<\\/");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to render portal bootstrap data", exception);
        }
    }

    private String escape(String value) {
        return value == null
                ? ""
                : value.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;");
    }

    private record PortalBootstrap(
            AccountBootstrap account,
            Collection<SyncSettingsResponse> connections
    ) {
    }

    private record AccountBootstrap(
            String userId,
            String email,
            String username,
            Set<String> providers
    ) {
    }

    public record PortalAlert(
            String tone,
            String title,
            String message,
            String detail,
            String actionHref,
            String actionText
    ) {
    }
}
