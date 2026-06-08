const bootstrap = window.H2TRAIN_BOOTSTRAP || { providers: [], connections: [] };
let connectionState = indexConnections(bootstrap.connections || []);
let transientActivity = [];

function normalizeProvider(providerId) {
    return providerId ? String(providerId).toLowerCase() : "";
}

function indexConnections(connections) {
    return connections.reduce(function(index, connection) {
        if (connection && connection.provider) {
            index[normalizeProvider(connection.provider)] = connection;
        }
        return index;
    }, {});
}

function readStoredConnections() {
    return Object.assign({}, connectionState);
}

function writeStoredConnections(connections) {
    connectionState = Object.assign({}, connections);
}

function currentConnection(providerId) {
    return readStoredConnections()[normalizeProvider(providerId)] || null;
}

function clearStoredConnection(providerId) {
    const connections = readStoredConnections();
    delete connections[normalizeProvider(providerId)];
    writeStoredConnections(connections);
}

function connectedConnections() {
    return Object.values(readStoredConnections()).filter(function(connection) {
        return Boolean(connection && connection.connected && connection.athleteId);
    });
}

function disconnectedState(providerId) {
    return {
        provider: providerId,
        connected: false,
        userId: null,
        athleteId: null,
        athleteUsername: "Waiting for authorization",
        syncEnabled: false,
        syncInterval: "EVERY_24_HOURS",
        syncIntervalLabel: "Every 24 hours",
        lastSyncedAt: null
    };
}

function providerDisplayName(providerId) {
    const provider = (bootstrap.providers || []).find(function(candidate) {
        return normalizeProvider(candidate.providerId) === normalizeProvider(providerId);
    });
    if (provider && provider.displayName) {
        return provider.displayName;
    }
    return providerId ? providerId.charAt(0).toUpperCase() + providerId.slice(1) : "Provider";
}

function formatLastSync(lastSyncedAt) {
    if (!lastSyncedAt) {
        return "Not synced yet";
    }
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(new Date(lastSyncedAt));
}

function latestSyncLabel() {
    const dates = connectedConnections()
        .map(function(connection) { return connection.lastSyncedAt; })
        .filter(Boolean)
        .map(function(value) { return new Date(value); })
        .sort(function(left, right) { return right - left; });
    return dates.length ? formatLastSync(dates[0].toISOString()) : "\u2014";
}

function setText(root, selector, value) {
    const element = root.querySelector(selector);
    if (element) {
        element.textContent = value;
    }
}

function setBadge(element, text, tone) {
    if (!element) {
        return;
    }
    element.textContent = text;
    element.dataset.tone = tone || "";
}

function syncHealth(connection) {
    if (!connection || !connection.connected) {
        return { text: "Pending", tone: "warning" };
    }
    if (!connection.syncEnabled) {
        return { text: "Paused", tone: "warning" };
    }
    return { text: "Healthy", tone: "success" };
}

function renderProviderSurface(surface, connection) {
    const providerId = normalizeProvider(surface.dataset.providerId);
    const effectiveState = connection && connection.connected
        ? connection
        : disconnectedState(providerId);
    const isConnected = Boolean(connection && connection.connected && connection.athleteId);
    const health = syncHealth(connection);

    setBadge(
        surface.querySelector("[data-role='connection-badge']"),
        isConnected ? "Connected" : "Not connected",
        isConnected ? "success" : "warning"
    );
    setText(surface, "[data-role='athlete']", effectiveState.athleteUsername || "Authorized athlete");
    setText(surface, "[data-role='consent']", isConnected ? "Granted" : "Pending");
    setText(surface, "[data-role='last-sync']", formatLastSync(effectiveState.lastSyncedAt));
    setText(surface, "[data-role='sync-interval']", effectiveState.syncIntervalLabel || "\u2014");
    setText(surface, "[data-role='sync-health']", health.text);
    const healthElement = surface.querySelector("[data-role='sync-health']");
    if (healthElement) {
        healthElement.dataset.tone = health.tone;
    }

    const syncNowButton = surface.querySelector("[data-action='sync-provider']");
    if (syncNowButton) {
        syncNowButton.disabled = !isConnected;
    }

    const connectLink = surface.querySelector(".connect-link");
    if (connectLink) {
        connectLink.textContent = isConnected ? "Reconnect" : "Connect";
        connectLink.href = buildConnectHref(providerId);
    }

    const toggleButton = surface.querySelector(".sync-toggle");
    const intervalSelect = surface.querySelector(".sync-interval");
    if (toggleButton) {
        toggleButton.disabled = !isConnected;
        toggleButton.setAttribute("aria-checked", String(Boolean(effectiveState.syncEnabled)));
        toggleButton.textContent = effectiveState.syncEnabled ? "Sync on" : "Sync off";
    }
    if (intervalSelect) {
        intervalSelect.value = effectiveState.syncInterval || "EVERY_24_HOURS";
        intervalSelect.disabled = !isConnected || !effectiveState.syncEnabled;
    }

    const status = surface.querySelector("[data-role='status-copy']");
    if (!status) {
        return;
    }
    if (!isConnected) {
        status.dataset.tone = "";
        status.textContent = "Authorize the provider to remember consent and enable automatic sync controls.";
        return;
    }
    if (effectiveState.syncEnabled) {
        status.dataset.tone = "ready";
        status.textContent = "Automatic sync is enabled and will run "
            + (effectiveState.syncIntervalLabel || "on the selected interval").toLowerCase()
            + ".";
        return;
    }
    status.dataset.tone = "paused";
    status.textContent = "Consent is stored, but automatic sync is paused until you enable the toggle again.";
}

function renderAllProviderSurfaces() {
    const connections = readStoredConnections();
    Array.from(document.querySelectorAll("[data-provider-id]")).forEach(function(surface) {
        renderProviderSurface(surface, connections[normalizeProvider(surface.dataset.providerId)] || null);
    });
    renderDashboardMetrics();
    renderActivityTimeline();
    renderRecentSyncTable();
}

async function fetchSyncSettings(providerId, athleteId) {
    const response = await window.fetch("/auth/" + encodeURIComponent(providerId) + "/athletes/" + encodeURIComponent(athleteId) + "/sync-settings", {
        headers: { "Accept": "application/json" }
    });

    if (response.status === 401) {
        window.location.assign("/login?error=session_required");
        return null;
    }
    if (response.status === 404) {
        clearStoredConnection(providerId);
        return null;
    }
    if (!response.ok) {
        throw new Error("Unable to load sync settings");
    }
    return response.json();
}

async function updateSyncSettings(providerId, athleteId, payload) {
    const response = await window.fetch("/auth/" + encodeURIComponent(providerId) + "/athletes/" + encodeURIComponent(athleteId) + "/sync-settings", {
        method: "PUT",
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    });

    if (response.status === 401) {
        window.location.assign("/login?error=session_required");
        return null;
    }
    if (response.status === 404) {
        clearStoredConnection(providerId);
        return null;
    }
    if (!response.ok) {
        throw new Error("Unable to update sync settings");
    }
    return response.json();
}

async function syncProviderNow(providerId, athleteId) {
    const response = await window.fetch("/auth/" + encodeURIComponent(providerId) + "/athletes/" + encodeURIComponent(athleteId) + "/sync", {
        headers: { "Accept": "application/json" }
    });

    if (response.status === 401) {
        let errorBody = null;
        try {
            errorBody = await response.json();
        } catch (ignored) {
            /* session errors may not include a JSON body */
        }
        if (errorBody && errorBody.provider) {
            throw new Error(errorBody.error || "Provider authorization expired. Reconnect the provider.");
        }
        window.location.assign("/login?error=session_required");
        return null;
    }
    if (response.status === 404) {
        clearStoredConnection(providerId);
        return null;
    }
    if (!response.ok) {
        let message = "Manual sync could not be completed.";
        try {
            const error = await response.json();
            if (error && error.error) {
                message = error.error;
            }
        } catch (ignored) {
            /* keep default message */
        }
        throw new Error(message);
    }
    return response.json();
}

function buildConnectHref(providerId) {
    return "/auth/" + encodeURIComponent(providerId) + "/login";
}

async function persistSettings(providerId, athleteId, payload, errorText) {
    const previousConnection = currentConnection(providerId);
    try {
        const updatedConnection = await updateSyncSettings(providerId, athleteId, payload);
        if (!updatedConnection) {
            renderAllProviderSurfaces();
            return;
        }
        const connections = readStoredConnections();
        connections[normalizeProvider(providerId)] = updatedConnection;
        writeStoredConnections(connections);
        renderAllProviderSurfaces();
    } catch (error) {
        const connections = readStoredConnections();
        if (previousConnection) {
            connections[normalizeProvider(providerId)] = previousConnection;
            writeStoredConnections(connections);
        }
        renderAllProviderSurfaces();
        showProviderStatus(providerId, "error", errorText);
    }
}

function showProviderStatus(providerId, tone, text) {
    Array.from(document.querySelectorAll("[data-provider-id='" + normalizeProvider(providerId) + "'] [data-role='status-copy']")).forEach(function(status) {
        status.dataset.tone = tone || "";
        status.textContent = text;
    });
}

function bindProviderSurface(surface) {
    if (surface.dataset.bound === "true") {
        return;
    }
    surface.dataset.bound = "true";
    const providerId = normalizeProvider(surface.dataset.providerId);
    const connectLink = surface.querySelector(".connect-link");
    const toggleButton = surface.querySelector(".sync-toggle");
    const intervalSelect = surface.querySelector(".sync-interval");
    const syncNowButton = surface.querySelector("[data-action='sync-provider']");
    const focusConfigButton = surface.querySelector("[data-action='focus-provider-config']");

    if (connectLink) {
        connectLink.addEventListener("click", function() {
            showProviderStatus(providerId, "", "Waiting for provider authorization...");
        });
    }

    if (toggleButton) {
        toggleButton.addEventListener("click", async function() {
            const connection = currentConnection(providerId);
            if (!connection || !connection.athleteId) {
                return;
            }

            await persistSettings(providerId, connection.athleteId, {
                enabled: !connection.syncEnabled,
                interval: intervalSelect ? intervalSelect.value : connection.syncInterval
            }, "The sync setting could not be updated. Try again.");
        });
    }

    if (intervalSelect) {
        intervalSelect.addEventListener("change", async function() {
            const connection = currentConnection(providerId);
            if (!connection || !connection.athleteId) {
                return;
            }

            await persistSettings(providerId, connection.athleteId, {
                enabled: toggleButton ? toggleButton.getAttribute("aria-checked") === "true" : connection.syncEnabled,
                interval: intervalSelect.value
            }, "The collection interval could not be updated. Try again.");
        });
    }

    if (syncNowButton) {
        syncNowButton.addEventListener("click", async function() {
            const connection = currentConnection(providerId);
            if (!connection || !connection.athleteId) {
                return;
            }
            const defaultContent = syncNowButton.innerHTML;
            syncNowButton.disabled = true;
            syncNowButton.textContent = "Syncing...";
            try {
                const result = await syncProviderNow(providerId, connection.athleteId);
                transientActivity.unshift({
                    provider: providerId,
                    status: "Completed",
                    events: result ? result.importedEvents : "\u2014",
                    started: new Date().toISOString(),
                    duration: "\u2014"
                });
                const refreshed = await fetchSyncSettings(providerId, connection.athleteId);
                if (refreshed) {
                    const connections = readStoredConnections();
                    connections[normalizeProvider(providerId)] = refreshed;
                    writeStoredConnections(connections);
                }
                renderAllProviderSurfaces();
                showProviderStatus(providerId, "ready", "Manual sync completed.");
            } catch (error) {
                renderAllProviderSurfaces();
                showProviderStatus(providerId, "error", error.message || "Manual sync could not be completed.");
            } finally {
                syncNowButton.innerHTML = defaultContent;
            }
        });
    }

    if (focusConfigButton) {
        focusConfigButton.addEventListener("click", function() {
            const panel = surface.querySelector(".sync-panel");
            if (panel) {
                panel.scrollIntoView({ behavior: "smooth", block: "center" });
            }
        });
    }
}

function setActiveSection(sectionName) {
    const normalized = sectionName || "dashboards";
    Array.from(document.querySelectorAll(".nav-item")).forEach(function(button) {
        button.classList.toggle("is-active", button.dataset.section === normalized);
    });
    Array.from(document.querySelectorAll("[data-section-panel]")).forEach(function(panel) {
        const active = panel.dataset.sectionPanel === normalized;
        panel.classList.toggle("is-active", active);
        panel.hidden = !active;
    });
    if (window.location.hash !== "#" + normalized) {
        window.history.replaceState({}, document.title, window.location.pathname + window.location.search + "#" + normalized);
    }
}

function bindNavigation() {
    Array.from(document.querySelectorAll(".nav-item")).forEach(function(button) {
        button.addEventListener("click", function() {
            setActiveSection(button.dataset.section);
        });
    });
    Array.from(document.querySelectorAll("[data-action='navigate']")).forEach(function(button) {
        button.addEventListener("click", function() {
            setActiveSection(button.dataset.targetSection);
        });
    });
}

function renderDashboardMetrics() {
    setText(document, "[data-kpi='connected-providers']", String(connectedConnections().length));
    setText(document, "[data-kpi='last-sync']", latestSyncLabel());
    const importedTotal = transientActivity
        .map(function(activity) { return Number(activity.events); })
        .filter(function(value) { return Number.isFinite(value); })
        .reduce(function(total, value) { return total + value; }, 0);
    if (importedTotal > 0) {
        setText(document, "[data-kpi='total-events']", String(importedTotal));
    }
}

function syncRowsFromConnections() {
    const rows = connectedConnections()
        .filter(function(connection) { return Boolean(connection.lastSyncedAt); })
        .map(function(connection) {
            return {
                provider: connection.provider,
                status: "Completed",
                events: "\u2014",
                started: connection.lastSyncedAt,
                duration: "\u2014"
            };
        });
    return transientActivity.concat(rows).slice(0, 8);
}

function renderActivityTimeline() {
    const timeline = document.querySelector("[data-role='activity-timeline']");
    if (!timeline) {
        return;
    }
    const rows = syncRowsFromConnections();
    if (!rows.length) {
        timeline.innerHTML = "<div class=\"empty-state\">No sync activity yet. Connect a provider or run a manual sync.</div>";
        return;
    }
    const items = rows.slice(0, 5).map(function(row) {
        const providerName = providerDisplayName(row.provider);
        return "<div class=\"timeline-item\">"
            + "<span class=\"timeline-icon timeline-icon-check\" aria-hidden=\"true\"></span>"
            + "<div><strong>" + escapeHtml(providerName + " sync " + row.status.toLowerCase()) + "</strong>"
            + "<p>" + escapeHtml(formatLastSync(row.started)) + "</p></div>"
            + "</div>";
    });
    items.push("<div class=\"timeline-item muted\"><span class=\"timeline-icon timeline-icon-info\" aria-hidden=\"true\"></span><div><strong>System check</strong><p>Portal services are online.</p></div></div>");
    timeline.innerHTML = items.join("");
}

function renderRecentSyncTable() {
    const tableBody = document.querySelector("[data-role='recent-sync-table']");
    if (!tableBody) {
        return;
    }
    const rows = syncRowsFromConnections();
    if (!rows.length) {
        tableBody.innerHTML = "<tr><td colspan=\"5\" class=\"empty-table\">No recent sync activity.</td></tr>";
        return;
    }
    tableBody.innerHTML = rows.map(function(row) {
        return "<tr>"
            + "<td>" + escapeHtml(providerDisplayName(row.provider)) + "</td>"
            + "<td><span class=\"status-badge\" data-tone=\"success\">" + escapeHtml(row.status) + "</span></td>"
            + "<td>" + escapeHtml(String(row.events)) + "</td>"
            + "<td>" + escapeHtml(formatLastSync(row.started)) + "</td>"
            + "<td>" + escapeHtml(row.duration) + "</td>"
            + "</tr>";
    }).join("");
}

function bindQuickActions() {
    const syncAllButton = document.querySelector("[data-action='sync-all']");
    const status = document.querySelector("[data-role='quick-action-status']");
    if (!syncAllButton) {
        return;
    }
    syncAllButton.addEventListener("click", async function() {
        const connections = connectedConnections();
        if (!connections.length) {
            if (status) {
                status.textContent = "Connect a provider before running sync.";
                status.dataset.tone = "warning";
            }
            return;
        }
        const defaultContent = syncAllButton.innerHTML;
        syncAllButton.disabled = true;
        syncAllButton.textContent = "Syncing...";
        if (status) {
            status.textContent = "Syncing connected providers...";
            status.dataset.tone = "";
        }
        try {
            for (const connection of connections) {
                const result = await syncProviderNow(connection.provider, connection.athleteId);
                transientActivity.unshift({
                    provider: connection.provider,
                    status: "Completed",
                    events: result ? result.importedEvents : "\u2014",
                    started: new Date().toISOString(),
                    duration: "\u2014"
                });
                const refreshed = await fetchSyncSettings(connection.provider, connection.athleteId);
                if (refreshed) {
                    const updatedConnections = readStoredConnections();
                    updatedConnections[normalizeProvider(connection.provider)] = refreshed;
                    writeStoredConnections(updatedConnections);
                }
            }
            if (status) {
                status.textContent = "Connected providers synced.";
                status.dataset.tone = "success";
            }
            renderAllProviderSurfaces();
        } catch (error) {
            if (status) {
                status.textContent = error.message || "Sync all could not be completed.";
                status.dataset.tone = "error";
            }
            renderAllProviderSurfaces();
        } finally {
            syncAllButton.disabled = false;
            syncAllButton.innerHTML = defaultContent;
        }
    });
}

function setCredentialStatus(form, tone, text) {
    const status = form.querySelector("[data-role='credential-status']");
    if (!status) {
        return;
    }
    status.dataset.tone = tone || "";
    status.textContent = text || "";
}

function bindCredentialForm(form) {
    form.addEventListener("input", function() {
        setCredentialStatus(form, "", "");
    });

    form.addEventListener("submit", function(event) {
        const formType = form.dataset.credentialForm;
        if (!form.checkValidity()) {
            return;
        }

        if (formType === "email") {
            const newEmail = form.elements.newEmail.value.trim().toLowerCase();
            const confirmNewEmail = form.elements.confirmNewEmail.value.trim().toLowerCase();
            const currentEmail = bootstrap.account && bootstrap.account.email
                ? bootstrap.account.email.trim().toLowerCase()
                : "";
            if (newEmail !== confirmNewEmail) {
                event.preventDefault();
                setCredentialStatus(form, "error", "The new email and confirmation email must match.");
                return;
            }
            if (currentEmail && newEmail === currentEmail) {
                event.preventDefault();
                setCredentialStatus(form, "error", "The new email must be different from your current email.");
            }
            return;
        }

        if (formType === "password") {
            const currentPassword = form.elements.currentPassword.value;
            const newPassword = form.elements.newPassword.value;
            const confirmNewPassword = form.elements.confirmNewPassword.value;
            if (newPassword !== confirmNewPassword) {
                event.preventDefault();
                setCredentialStatus(form, "error", "The new password and confirmation password must match.");
                return;
            }
            if (currentPassword === newPassword) {
                event.preventDefault();
                setCredentialStatus(form, "error", "The new password must be different from your current password.");
            }
        }
    });
}

async function syncStoredConnections() {
    const connections = readStoredConnections();
    const providerIds = Object.keys(connections);

    await Promise.all(providerIds.map(async function(providerId) {
        const current = connections[providerId];
        if (!current || !current.athleteId) {
            return;
        }

        try {
            const freshConnection = await fetchSyncSettings(providerId, current.athleteId);
            if (freshConnection) {
                connections[providerId] = freshConnection;
            } else {
                delete connections[providerId];
            }
        } catch (error) {
            /* keep local state if the backend is temporarily unavailable */
        }
    }));

    writeStoredConnections(connections);
}

async function processCallbackConnection() {
    const url = new URL(window.location.href);
    const transientKeys = ["connectedProvider", "athleteId", "providerError", "provider", "accountStatus", "accountError"];
    const hasTransientParam = transientKeys.some(function(key) {
        return url.searchParams.has(key);
    });

    if (!hasTransientParam) {
        return;
    }

    transientKeys.forEach(function(key) {
        url.searchParams.delete(key);
    });

    const remainingQuery = url.searchParams.toString();
    window.history.replaceState(
        {},
        document.title,
        url.pathname + (remainingQuery ? "?" + remainingQuery : "") + url.hash
    );
}

function escapeHtml(value) {
    return value == null
        ? ""
        : String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
}

function bindAuthInteractions() {
    Array.from(document.querySelectorAll("[data-action='toggle-password']")).forEach(function(button) {
        if (button.dataset.bound === "true") {
            return;
        }
        button.dataset.bound = "true";
        button.addEventListener("click", function() {
            const target = document.getElementById(button.dataset.target);
            if (!target) {
                return;
            }
            const shouldShow = target.type === "password";
            target.type = shouldShow ? "text" : "password";
            button.classList.toggle("is-visible", shouldShow);
            button.setAttribute("aria-label", shouldShow ? "Hide password" : "Show password");
        });
    });
}

async function boot() {
    bindAuthInteractions();
    if (!document.querySelector(".portal-app")) {
        return;
    }

    await processCallbackConnection();
    await syncStoredConnections();

    bindNavigation();
    bindQuickActions();
    Array.from(document.querySelectorAll("[data-provider-id]")).forEach(bindProviderSurface);
    Array.from(document.querySelectorAll(".credential-form")).forEach(bindCredentialForm);
    renderAllProviderSurfaces();

    const hashSection = window.location.hash ? window.location.hash.substring(1) : "dashboards";
    setActiveSection(["dashboards", "providers", "settings"].includes(hashSection) ? hashSection : "dashboards");
}

window.addEventListener("DOMContentLoaded", boot);
