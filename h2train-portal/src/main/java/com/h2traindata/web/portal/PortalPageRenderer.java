package com.h2traindata.web.portal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.h2traindata.domain.InternalUserAccount;
import com.h2traindata.domain.SyncInterval;
import com.h2traindata.web.dto.SyncSettingsResponse;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
        List<ProviderPortalDescriptor> descriptors = providerIds.stream()
                .sorted()
                .map(descriptorFactory::create)
                .toList();
        Map<String, SyncSettingsResponse> connectionsByProvider = connections.stream()
                .collect(Collectors.toMap(
                        SyncSettingsResponse::provider,
                        Function.identity(),
                        (left, right) -> left
                ));
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
                    <div class="portal-app">
                        <aside class="portal-sidebar" aria-label="Main navigation">
                            <div class="sidebar-brand">
                                <img class="sidebar-logo" src="/h2train-logo.png" alt="H2Train logo">
                                <div>
                                    <strong>H2Train Data</strong>
                                    <span>Data Ingestion Portal</span>
                                </div>
                            </div>
                            <nav class="sidebar-nav">
                                <button class="nav-item is-active" type="button" data-section="dashboards">
                                    <span class="nav-icon" aria-hidden="true">__ICON_DASHBOARD__</span>
                                    <span>Dashboards</span>
                                </button>
                                <button class="nav-item" type="button" data-section="providers">
                                    <span class="nav-icon" aria-hidden="true">__ICON_PROVIDERS__</span>
                                    <span>Providers</span>
                                </button>
                                <button class="nav-item" type="button" data-section="settings">
                                    <span class="nav-icon" aria-hidden="true">__ICON_SETTINGS__</span>
                                    <span>Settings</span>
                                </button>
                            </nav>
                            <div class="sidebar-account-card">
                                <span class="system-pill" data-role="system-status">System online</span>
                                <strong>%s</strong>
                                <span>%s</span>
                            </div>
                        </aside>

                        <main class="portal-main">
                            __PORTAL_ALERT__
                            <section class="portal-section is-active" data-section-panel="dashboards">
                                <header class="page-header">
                                    <div>
                                        <span class="page-kicker">Dashboards</span>
                                        <h1>Welcome back, %s</h1>
                                        <p>Monitor provider ingestion, sync health, and longitudinal data readiness from one place.</p>
                                    </div>
                                    <div class="top-account-card">
                                        <div>
                                            <strong>%s</strong>
                                            <span>%s</span>
                                        </div>
                                        <form method="post" action="/account/logout">
                                            <button class="secondary-action" type="submit">__ICON_LOGOUT__ Sign out</button>
                                        </form>
                                    </div>
                                </header>

                                <div class="kpi-grid">
                                    <article class="kpi-card">
                                        <span class="kpi-icon kpi-icon-users" aria-hidden="true">__ICON_USERS__</span>
                                        <div class="kpi-copy">
                                            <span>Connected Providers</span>
                                            <strong data-kpi="connected-providers">%s</strong>
                                            <small>Active OAuth links</small>
                                        </div>
                                    </article>
                                    <article class="kpi-card">
                                        <span class="kpi-icon kpi-icon-events" aria-hidden="true">__ICON_DATABASE__</span>
                                        <div class="kpi-copy">
                                            <span>Total Events</span>
                                            <strong data-kpi="total-events">\u2014</strong>
                                            <small>Available after event aggregation</small>
                                        </div>
                                    </article>
                                    <article class="kpi-card kpi-card-last-sync">
                                        <span class="kpi-icon kpi-icon-sync" aria-hidden="true">__ICON_CLOCK__</span>
                                        <div class="kpi-copy">
                                            <span>Last Sync</span>
                                            <strong data-kpi="last-sync">%s</strong>
                                            <small>Most recent provider update</small>
                                        </div>
                                    </article>
                                </div>

                                <div class="dashboard-grid">
                                    <article class="dashboard-card timeline-card">
                                        <div class="card-header">
                                            <div>
                                                <h2><span class="section-title-icon" aria-hidden="true">__ICON_CALENDAR__</span>Activity Timeline</h2>
                                                <p>Recent provider sync activity.</p>
                                            </div>
                                        </div>
                                        <div class="timeline-list" data-role="activity-timeline">
                                            <div class="empty-state">No sync activity yet. Connect a provider or run a manual sync.</div>
                                        </div>
                                    </article>

                                    <article class="dashboard-card quick-actions-card">
                                        <div class="card-header">
                                            <div>
                                                <h2><span class="section-title-icon" aria-hidden="true">__ICON_ZAP__</span>Quick Actions</h2>
                                                <p>Common actions for this portal.</p>
                                            </div>
                                        </div>
                                        <div class="quick-actions">
                                            <button class="primary-action" type="button" data-action="sync-all">__ICON_REFRESH__ Sync all now</button>
                                            <button class="secondary-action" type="button" data-action="navigate" data-target-section="providers">__ICON_PROVIDERS__ View providers</button>
                                            <button class="secondary-action" type="button" data-action="navigate" data-target-section="settings">__ICON_SETTINGS__ Settings</button>
                                        </div>
                                        <p class="quick-action-status" data-role="quick-action-status" aria-live="polite"></p>
                                    </article>
                                </div>

                                <section class="dashboard-card">
                                    <div class="card-header">
                                        <div>
                                            <h2><span class="section-title-icon" aria-hidden="true">__ICON_PROVIDERS__</span>Providers Status</h2>
                                            <p>Connection status and sync health for supported providers.</p>
                                        </div>
                                    </div>
                                    <div class="provider-status-grid">
                                        __DASHBOARD_PROVIDER_CARDS__
                                    </div>
                                </section>

                                <section class="dashboard-card">
                                    <div class="card-header">
                                        <div>
                                            <h2><span class="section-title-icon" aria-hidden="true">__ICON_REFRESH__</span>Recent Sync Activity</h2>
                                            <p>Latest provider runs based on current connection state.</p>
                                        </div>
                                    </div>
                                    <div class="table-wrap">
                                        <table class="sync-table">
                                            <thead>
                                                <tr>
                                                    <th>Provider</th>
                                                    <th>Status</th>
                                                    <th>Events</th>
                                                    <th>Started</th>
                                                    <th>Duration</th>
                                                </tr>
                                            </thead>
                                            <tbody data-role="recent-sync-table">
                                                <tr>
                                                    <td colspan="5" class="empty-table">No recent sync activity.</td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </section>
                            </section>

                            <section class="portal-section" data-section-panel="providers" hidden>
                                <header class="page-header">
                                    <div>
                                        <span class="page-kicker">Providers</span>
                                        <h1>Provider connections</h1>
                                        <p>Connect, reconnect, sync, and configure automatic ingestion for Fitbit and Strava.</p>
                                    </div>
                                </header>
                                <div class="provider-grid">
                                    __PROVIDER_CARDS__
                                </div>
                            </section>

                            <section class="portal-section" data-section-panel="settings" hidden>
                                <header class="page-header">
                                    <div>
                                        <span class="page-kicker">Settings</span>
                                        <h1>Account settings</h1>
                                        <p>Manage your internal H2Train account and review connected providers.</p>
                                    </div>
                                </header>
                                __SETTINGS_CONTENT__
                            </section>
                        </main>
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
                escape(accountUsername),
                escape(accountUsername),
                escape(accountEmail),
                connectedProviderCount(connections),
                escape(lastSyncLabel(connections))
        )
                .replace("__DASHBOARD_PROVIDER_CARDS__", renderDashboardProviderCards(descriptors, connectionsByProvider))
                .replace("__PROVIDER_CARDS__", renderProviderCards(descriptors, connectionsByProvider))
                .replace("__SETTINGS_CONTENT__", renderSettings(userAccount, descriptors, connectionsByProvider))
                .replace("__PORTAL_ALERT__", renderAlert(alert))
                .replace("__BOOTSTRAP__", bootstrapJson(userAccount, connections, descriptors))
                .replace("__ICON_DASHBOARD__", icon("dashboard"))
                .replace("__ICON_PROVIDERS__", icon("providers"))
                .replace("__ICON_SETTINGS__", icon("settings"))
                .replace("__ICON_LOGOUT__", icon("logout"))
                .replace("__ICON_USERS__", icon("users"))
                .replace("__ICON_DATABASE__", icon("database"))
                .replace("__ICON_CLOCK__", icon("clock"))
                .replace("__ICON_CALENDAR__", icon("calendar"))
                .replace("__ICON_ZAP__", icon("zap"))
                .replace("__ICON_REFRESH__", icon("refresh"))
                .replace("__ICON_GEAR__", icon("gear"));
    }

    private String renderDashboardProviderCards(List<ProviderPortalDescriptor> descriptors,
                                                Map<String, SyncSettingsResponse> connectionsByProvider) {
        return descriptors.stream()
                .map(descriptor -> {
                    SyncSettingsResponse connection = connectionsByProvider.get(descriptor.providerId());
                    return """
                            <article class="provider-status-card" style="%s" data-provider-id="%s">
                                <div class="provider-status-top">
                                    <div class="provider-logo">%s</div>
                                    <div>
                                        <h3>%s</h3>
                                        <span class="status-badge" data-role="connection-badge" data-tone="%s">%s</span>
                                    </div>
                                </div>
                                <div class="provider-facts">
                                    <span>Athlete</span><strong data-role="athlete">%s</strong>
                                    <span>Last Sync</span><strong data-role="last-sync">%s</strong>
                                    <span>Sync Interval</span><strong data-role="sync-interval">%s</strong>
                                    <span>Sync Health</span><strong class="status-badge" data-role="sync-health" data-tone="%s">%s</strong>
                                </div>
                                <div class="provider-actions">
                                    <button class="primary-action sync-now" type="button" data-action="sync-provider" disabled>__ICON_REFRESH__ Sync Now</button>
                                    <button class="secondary-action" type="button" data-action="navigate" data-target-section="providers">__ICON_GEAR__ Configure</button>
                                </div>
                            </article>
                            """.formatted(
                            descriptor.themeStyle(),
                            descriptor.domProviderId(),
                            descriptor.logoMarkup(),
                            escape(descriptor.displayName()),
                            connectionTone(connection),
                            connectionBadge(connection),
                            escape(athlete(connection)),
                            escape(lastSync(connection)),
                            escape(interval(connection)),
                            syncHealthTone(connection),
                            escape(syncHealth(connection))
                    );
                })
                .collect(Collectors.joining());
    }

    private String renderProviderCards(List<ProviderPortalDescriptor> descriptors,
                                       Map<String, SyncSettingsResponse> connectionsByProvider) {
        return descriptors.stream()
                .map(descriptor -> {
                    SyncSettingsResponse connection = connectionsByProvider.get(descriptor.providerId());
                    return """
                            <article class="provider-card" style="%s" data-provider-id="%s">
                                <div class="provider-card-hero">
                                    <div class="provider-logo provider-logo-large">%s</div>
                                    <div>
                                        <span class="provider-tag">Provider Sync</span>
                                        <h2>%s</h2>
                                        <p>%s</p>
                                    </div>
                                    <span class="status-badge" data-role="connection-badge" data-tone="%s">%s</span>
                                </div>
                                <div class="provider-detail-grid">
                                    <div><span>Athlete</span><strong data-role="athlete">%s</strong></div>
                                    <div><span>Consent</span><strong data-role="consent">%s</strong></div>
                                    <div><span>Last Sync</span><strong data-role="last-sync">%s</strong></div>
                                    <div><span>Sync Interval</span><strong data-role="sync-interval">%s</strong></div>
                                    <div><span>Sync Health</span><strong class="status-badge" data-role="sync-health" data-tone="%s">%s</strong></div>
                                </div>
                                <div class="data-type-list">
                                    %s
                                </div>
                                <div class="sync-panel">
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
                                    <p class="provider-status" data-role="status-copy">%s</p>
                                </div>
                                <div class="provider-card-actions">
                                    <button class="primary-action sync-now" type="button" data-action="sync-provider" disabled>__ICON_REFRESH__ Sync Now</button>
                                    <button class="secondary-action" type="button" data-action="focus-provider-config">__ICON_GEAR__ Configure</button>
                                    <a class="connect-link" href="/auth/%s/login">%s</a>
                                </div>
                            </article>
                            """.formatted(
                            descriptor.themeStyle(),
                            descriptor.domProviderId(),
                            descriptor.logoMarkup(),
                            escape(descriptor.displayName()),
                            escape(descriptor.description()),
                            connectionTone(connection),
                            connectionBadge(connection),
                            escape(athlete(connection)),
                            connection != null && connection.connected() ? "Granted" : "Pending",
                            escape(lastSync(connection)),
                            escape(interval(connection)),
                            syncHealthTone(connection),
                            escape(syncHealth(connection)),
                            renderDataTypes(descriptor.providerId()),
                            renderIntervalOptions(),
                            connection != null && connection.connected()
                                    ? "Provider is connected. Configure automatic sync or run a manual sync."
                                    : "Authorize the provider to enable ingestion and automatic sync controls.",
                            escape(descriptor.providerId()),
                            connection != null && connection.connected() ? "Reconnect" : "Connect"
                    );
                })
                .collect(Collectors.joining());
    }

    private String renderSettings(InternalUserAccount userAccount,
                                  List<ProviderPortalDescriptor> descriptors,
                                  Map<String, SyncSettingsResponse> connectionsByProvider) {
        if (userAccount == null) {
            return "";
        }
        String connectedProviders = descriptors.stream()
                .map(descriptor -> {
                    SyncSettingsResponse connection = connectionsByProvider.get(descriptor.providerId());
                    return """
                            <div class="settings-provider-row" data-provider-id="%s">
                                <span>%s</span>
                                <strong class="status-badge" data-tone="%s">%s</strong>
                            </div>
                            """.formatted(
                            descriptor.domProviderId(),
                            escape(descriptor.displayName()),
                            connectionTone(connection),
                            connectionBadge(connection)
                    );
                })
                .collect(Collectors.joining());

        return """
                <div class="settings-layout">
                    <article class="settings-card account-summary-card">
                        <span class="provider-tag">Internal account</span>
                        <h2>Account information</h2>
                        <dl class="account-definition-list">
                            <div><dt>Username</dt><dd>%s</dd></div>
                            <div><dt>Email</dt><dd>%s</dd></div>
                            <div><dt>Sign-in method</dt><dd>%s</dd></div>
                            <div><dt>User ID</dt><dd>%s</dd></div>
                        </dl>
                    </article>
                    <article class="settings-card">
                        <span class="provider-tag">Security</span>
                        <h2>Connected providers</h2>
                        <div class="settings-provider-list">
                            %s
                        </div>
                    </article>
                    %s
                </div>
                """.formatted(
                escape(userAccount.username()),
                escape(userAccount.email()),
                escape(signInMethod(userAccount)),
                escape(userAccount.id()),
                connectedProviders,
                renderCredentialSettings(userAccount)
        );
    }

    private String renderCredentialSettings(InternalUserAccount userAccount) {
        if (!userAccount.hasLocalCredentials()) {
            return """
                    <article class="settings-card externally-managed-card">
                        <span class="provider-tag">Google account</span>
                        <h2>Google-managed sign-in</h2>
                        <p class="settings-current">
                            This account uses Google as its identity provider. Email changes, password changes, and password recovery are managed by Google.
                        </p>
                        <a class="secondary-action" href="https://myaccount.google.com/security" rel="noopener noreferrer">Open Google security settings</a>
                    </article>
                    """;
        }
        return renderEmailSettings(userAccount) + renderPasswordSettings(userAccount);
    }

    private String renderEmailSettings(InternalUserAccount userAccount) {
        return """
                <article class="settings-card">
                    <span class="provider-tag">Internal account</span>
                    <h2>Change email address</h2>
                    <p class="settings-current">Current email <strong>%s</strong></p>
                    <form class="credential-form" data-credential-form="email" method="post" action="/account/email">
                        <label>
                            <span>New email</span>
                            <input name="newEmail" type="email" autocomplete="email" required>
                        </label>
                        <label>
                            <span>Confirm new email</span>
                            <input name="confirmNewEmail" type="email" autocomplete="email" required>
                        </label>
                        <label>
                            <span>Current password</span>
                            <input name="currentPassword" type="password" autocomplete="current-password" required>
                        </label>
                        <p class="credential-status" data-role="credential-status" aria-live="polite"></p>
                        <button class="primary-action" type="submit">Save email</button>
                    </form>
                </article>
                """.formatted(escape(userAccount.email()));
    }

    private String renderPasswordSettings(InternalUserAccount userAccount) {
        return """
                <article class="settings-card">
                    <span class="provider-tag">Security</span>
                    <h2>Change password</h2>
                    <p class="settings-current">Use at least 8 characters and confirm the new password.</p>
                    <form class="credential-form" data-credential-form="password" method="post" action="/account/password">
                        <label>
                            <span>Current password</span>
                            <input name="currentPassword" type="password" autocomplete="current-password" required>
                        </label>
                        <label>
                            <span>New password</span>
                            <input name="newPassword" type="password" autocomplete="new-password" minlength="8" required>
                        </label>
                        <label>
                            <span>Confirm new password</span>
                            <input name="confirmNewPassword" type="password" autocomplete="new-password" minlength="8" required>
                        </label>
                        <p class="credential-status" data-role="credential-status" aria-live="polite"></p>
                        <button class="primary-action" type="submit">Save password</button>
                    </form>
                    <div class="password-recovery-panel">
                        <div>
                            <h3>Recover password</h3>
                            <p>Send a recovery link to <strong>%s</strong> to set a new password without using the current one.</p>
                        </div>
                        <form method="post" action="/account/password/reset/request">
                            <button class="secondary-action" type="submit">Send recovery email</button>
                        </form>
                    </div>
                </article>
                """.formatted(escape(userAccount.email()));
    }

    private String signInMethod(InternalUserAccount userAccount) {
        return userAccount.hasLocalCredentials() ? "H2Train password" : "Google";
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

    private String renderIntervalOptions() {
        return java.util.Arrays.stream(SyncInterval.values())
                .map(interval -> "                                            <option value=\""
                        + interval.name()
                        + "\">"
                        + interval.label()
                        + "</option>")
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String renderDataTypes(String providerId) {
        List<String> dataTypes = switch (providerId.toLowerCase(java.util.Locale.ROOT)) {
            case "fitbit" -> List.of("Activity", "Heart rate", "Sleep", "Body composition");
            case "strava" -> List.of("Activities", "Distance", "Calories", "Athlete profile");
            default -> List.of("Events", "Metrics");
        };
        return dataTypes.stream()
                .map(type -> "<span class=\"data-type-chip\" title=\""
                        + escape(type)
                        + "\" aria-label=\""
                        + escape(type)
                        + "\">"
                        + icon(dataTypeIcon(type))
                        + "</span>")
                .collect(Collectors.joining());
    }

    private String dataTypeIcon(String type) {
        return switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "activity", "activities" -> "activity";
            case "heart rate" -> "heart";
            case "sleep" -> "moon";
            case "body composition" -> "scale";
            case "distance" -> "route";
            case "calories" -> "flame";
            case "athlete profile" -> "profile";
            default -> "database";
        };
    }

    private String icon(String name) {
        return switch (name) {
            case "dashboard" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M3 10.5 12 3l9 7.5"></path>
                        <path d="M5 9.5V21h14V9.5"></path>
                        <path d="M9 21v-6h6v6"></path>
                    </svg>
                    """;
            case "providers" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="6" cy="7" r="2.5"></circle>
                        <circle cx="18" cy="7" r="2.5"></circle>
                        <circle cx="12" cy="18" r="2.5"></circle>
                        <path d="m8.2 8.8 2.6 6.4"></path>
                        <path d="m15.8 8.8-2.6 6.4"></path>
                    </svg>
                    """;
            case "settings" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 15.5A3.5 3.5 0 1 0 12 8a3.5 3.5 0 0 0 0 7.5Z"></path>
                        <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 1.55V21a2 2 0 0 1-4 0v-.05a1.7 1.7 0 0 0-1-1.55 1.7 1.7 0 0 0-1.88.34l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.55-1H3a2 2 0 0 1 0-4h.05A1.7 1.7 0 0 0 4.6 9a1.7 1.7 0 0 0-.34-1.88l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-1.55V3a2 2 0 0 1 4 0v.05a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1.88-.34l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.7 1.7 0 0 0 19.4 9a1.7 1.7 0 0 0 1.55 1H21a2 2 0 0 1 0 4h-.05A1.7 1.7 0 0 0 19.4 15Z"></path>
                    </svg>
                    """;
            case "logout" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M10 17 15 12l-5-5"></path>
                        <path d="M15 12H3"></path>
                        <path d="M12 3h7v18h-7"></path>
                    </svg>
                    """;
            case "users" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M16 21v-2a4 4 0 0 0-4-4H7a4 4 0 0 0-4 4v2"></path>
                        <circle cx="9.5" cy="7" r="4"></circle>
                        <path d="M22 21v-2a4 4 0 0 0-3-3.87"></path>
                        <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                    </svg>
                    """;
            case "database" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <ellipse cx="12" cy="5" rx="8" ry="3"></ellipse>
                        <path d="M4 5v6c0 1.66 3.58 3 8 3s8-1.34 8-3V5"></path>
                        <path d="M4 11v6c0 1.66 3.58 3 8 3s8-1.34 8-3v-6"></path>
                    </svg>
                    """;
            case "clock" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="12" cy="12" r="9"></circle>
                        <path d="M12 7v5l3 2"></path>
                    </svg>
                    """;
            case "calendar" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M8 2v4"></path>
                        <path d="M16 2v4"></path>
                        <rect x="3" y="5" width="18" height="16" rx="2"></rect>
                        <path d="M3 10h18"></path>
                    </svg>
                    """;
            case "zap" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M13 2 4 14h7l-1 8 10-14h-7l1-6Z"></path>
                    </svg>
                    """;
            case "refresh" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M21 12a9 9 0 0 1-15.5 6.3"></path>
                        <path d="M3 12A9 9 0 0 1 18.5 5.7"></path>
                        <path d="M18 2v4h-4"></path>
                        <path d="M6 22v-4h4"></path>
                    </svg>
                    """;
            case "gear" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="12" cy="12" r="3"></circle>
                        <path d="M19 12a7 7 0 0 0-.1-1.2l2-1.5-2-3.5-2.4 1a7.2 7.2 0 0 0-2-1.1L14 3h-4l-.5 2.7a7.2 7.2 0 0 0-2 1.1l-2.4-1-2 3.5 2 1.5A7 7 0 0 0 5 12c0 .4 0 .8.1 1.2l-2 1.5 2 3.5 2.4-1a7.2 7.2 0 0 0 2 1.1L10 21h4l.5-2.7a7.2 7.2 0 0 0 2-1.1l2.4 1 2-3.5-2-1.5c.1-.4.1-.8.1-1.2Z"></path>
                    </svg>
                    """;
            case "activity" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M4 13h4l2-6 4 12 2-6h4"></path>
                    </svg>
                    """;
            case "heart" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 1 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z"></path>
                    </svg>
                    """;
            case "moon" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M20 14.7A8 8 0 0 1 9.3 4 8.5 8.5 0 1 0 20 14.7Z"></path>
                    </svg>
                    """;
            case "scale" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 3v18"></path>
                        <path d="M5 7h14"></path>
                        <path d="m6 7-3 6h6L6 7Z"></path>
                        <path d="m18 7-3 6h6l-3-6Z"></path>
                    </svg>
                    """;
            case "route" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="6" cy="18" r="3"></circle>
                        <circle cx="18" cy="6" r="3"></circle>
                        <path d="M9 18h3a4 4 0 0 0 0-8h-1a4 4 0 0 1 0-8h4"></path>
                    </svg>
                    """;
            case "flame" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 22a7 7 0 0 0 7-7c0-4-3-6.5-5-9-.5 2.8-2.5 4-4 5.5A5.7 5.7 0 0 0 8 15a4 4 0 0 0 8 0c0-1.5-.8-2.7-2-4"></path>
                    </svg>
                    """;
            case "profile" -> """
                    <svg class="ui-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="12" cy="8" r="4"></circle>
                        <path d="M4 21a8 8 0 0 1 16 0"></path>
                    </svg>
                    """;
            default -> "";
        };
    }

    private String bootstrapJson(InternalUserAccount userAccount,
                                 Collection<SyncSettingsResponse> connections,
                                 List<ProviderPortalDescriptor> descriptors) {
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
                    descriptors.stream()
                            .map(descriptor -> new ProviderBootstrap(
                                    descriptor.providerId(),
                                    descriptor.displayName(),
                                    dataTypes(descriptor.providerId())
                            ))
                            .toList(),
                    connections
            );
            return objectMapper.writeValueAsString(bootstrap).replace("</", "<\\/");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to render portal bootstrap data", exception);
        }
    }

    private List<String> dataTypes(String providerId) {
        return switch (providerId.toLowerCase(java.util.Locale.ROOT)) {
            case "fitbit" -> List.of("Activity", "Heart rate", "Sleep", "Body composition");
            case "strava" -> List.of("Activities", "Distance", "Calories", "Athlete profile");
            default -> List.of("Events", "Metrics");
        };
    }

    private String connectedProviderCount(Collection<SyncSettingsResponse> connections) {
        long count = connections.stream()
                .filter(SyncSettingsResponse::connected)
                .count();
        return String.valueOf(count);
    }

    private String lastSyncLabel(Collection<SyncSettingsResponse> connections) {
        return connections.stream()
                .map(SyncSettingsResponse::lastSyncedAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .map(Instant::toString)
                .orElse("\u2014");
    }

    private String connectionBadge(SyncSettingsResponse connection) {
        return connection != null && connection.connected() ? "Connected" : "Not connected";
    }

    private String connectionTone(SyncSettingsResponse connection) {
        return connection != null && connection.connected() ? "success" : "warning";
    }

    private String athlete(SyncSettingsResponse connection) {
        return connection != null && connection.athleteUsername() != null
                ? connection.athleteUsername()
                : "Waiting for authorization";
    }

    private String lastSync(SyncSettingsResponse connection) {
        return connection != null && connection.lastSyncedAt() != null ? connection.lastSyncedAt().toString() : "Not synced yet";
    }

    private String interval(SyncSettingsResponse connection) {
        return connection != null && connection.syncIntervalLabel() != null ? connection.syncIntervalLabel() : "\u2014";
    }

    private String syncHealth(SyncSettingsResponse connection) {
        if (connection == null || !connection.connected()) {
            return "Pending";
        }
        return connection.syncEnabled() ? "Healthy" : "Paused";
    }

    private String syncHealthTone(SyncSettingsResponse connection) {
        if (connection == null || !connection.connected()) {
            return "warning";
        }
        return connection.syncEnabled() ? "success" : "warning";
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
            Collection<ProviderBootstrap> providers,
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

    private record ProviderBootstrap(
            String providerId,
            String displayName,
            List<String> dataTypes
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
