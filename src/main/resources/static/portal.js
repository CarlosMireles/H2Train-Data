const storageKey = "h2train.sync.connections.v1";

function readStoredConnections() {
    try {
        const rawValue = window.localStorage.getItem(storageKey);
        return rawValue ? JSON.parse(rawValue) : {};
    } catch (error) {
        return {};
    }
}

function writeStoredConnections(connections) {
    window.localStorage.setItem(storageKey, JSON.stringify(connections));
}

function clearStoredConnection(providerId) {
    const connections = readStoredConnections();
    delete connections[providerId];
    writeStoredConnections(connections);
}

async function fetchSyncSettings(providerId, athleteId) {
    const response = await window.fetch("/auth/" + encodeURIComponent(providerId) + "/athletes/" + encodeURIComponent(athleteId) + "/sync-settings", {
        headers: { "Accept": "application/json" }
    });

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

    if (response.status === 404) {
        clearStoredConnection(providerId);
        return null;
    }
    if (!response.ok) {
        throw new Error("Unable to update sync settings");
    }
    return response.json();
}

function disconnectedState(providerId) {
    return {
        provider: providerId,
        connected: false,
        athleteId: null,
        athleteUsername: "Waiting for authorization",
        syncEnabled: false,
        syncInterval: "EVERY_24_HOURS",
        syncIntervalLabel: "Every 24 hours",
        lastSyncedAt: null
    };
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

function setStatus(card, tone, text) {
    const status = card.querySelector(".provider-status");
    status.dataset.tone = tone || "";
    status.textContent = text;
}

function renderCard(card, connection) {
    const connectLink = card.querySelector(".connect-link");
    const toggleButton = card.querySelector(".sync-toggle");
    const intervalSelect = card.querySelector(".sync-interval");
    const athleteValue = card.querySelector("[data-role='athlete']");
    const consentValue = card.querySelector("[data-role='consent']");
    const lastSyncValue = card.querySelector("[data-role='last-sync']");
    const isConnected = Boolean(connection && connection.connected && connection.athleteId);
    const effectiveState = isConnected ? connection : disconnectedState(card.dataset.providerId);

    connectLink.textContent = isConnected ? "Reconnect" : "Connect";
    athleteValue.textContent = effectiveState.athleteUsername || "Authorized athlete";
    consentValue.textContent = isConnected ? "Granted" : "Pending";
    lastSyncValue.textContent = formatLastSync(effectiveState.lastSyncedAt);
    toggleButton.disabled = !isConnected;
    toggleButton.setAttribute("aria-checked", String(Boolean(effectiveState.syncEnabled)));
    toggleButton.textContent = effectiveState.syncEnabled ? "Sync on" : "Sync off";
    intervalSelect.value = effectiveState.syncInterval || "EVERY_24_HOURS";
    intervalSelect.disabled = !isConnected || !effectiveState.syncEnabled;

    if (!isConnected) {
        setStatus(card, "", "Authorize the provider to remember consent and enable automatic sync controls.");
        return;
    }

    if (effectiveState.syncEnabled) {
        setStatus(
            card,
            "ready",
            "Automatic sync is enabled and will run " + (effectiveState.syncIntervalLabel || "on the selected interval").toLowerCase() + "."
        );
        return;
    }

    setStatus(card, "paused", "Consent is stored, but automatic sync is paused until you enable the toggle again.");
}

async function syncStoredConnections() {
    const connections = readStoredConnections();
    const providerIds = Object.keys(connections);

    await Promise.all(providerIds.map(async function(providerId) {
        const currentConnection = connections[providerId];
        if (!currentConnection || !currentConnection.athleteId) {
            return;
        }

        try {
            const freshConnection = await fetchSyncSettings(providerId, currentConnection.athleteId);
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
    const params = new URLSearchParams(window.location.search);
    const providerId = params.get("connectedProvider");
    const athleteId = params.get("athleteId");
    if (!providerId || !athleteId) {
        return;
    }

    const connections = readStoredConnections();
    try {
        const connection = await fetchSyncSettings(providerId, athleteId);
        if (connection) {
            connections[providerId] = connection;
            writeStoredConnections(connections);
        }
    } catch (error) {
        connections[providerId] = {
            provider: providerId,
            athleteId: athleteId,
            athleteUsername: "Authorized athlete",
            connected: true,
            syncEnabled: true,
            syncInterval: "EVERY_24_HOURS",
            syncIntervalLabel: "Every 24 hours",
            lastSyncedAt: null
        };
        writeStoredConnections(connections);
    }

    window.history.replaceState({}, document.title, window.location.pathname);
}

function currentConnection(providerId) {
    return readStoredConnections()[providerId] || null;
}

async function persistSettings(card, providerId, athleteId, payload, errorText) {
    const toggleButton = card.querySelector(".sync-toggle");
    const intervalSelect = card.querySelector(".sync-interval");
    const previousConnection = currentConnection(providerId);

    toggleButton.disabled = true;
    intervalSelect.disabled = true;
    try {
        const updatedConnection = await updateSyncSettings(providerId, athleteId, payload);
        if (!updatedConnection) {
            renderCard(card, null);
            return;
        }

        const connections = readStoredConnections();
        connections[providerId] = updatedConnection;
        writeStoredConnections(connections);
        renderCard(card, updatedConnection);
    } catch (error) {
        renderCard(card, previousConnection);
        setStatus(card, "error", errorText);
    }
}

function bindCard(card) {
    const providerId = card.dataset.providerId;
    const connectLink = card.querySelector(".connect-link");
    const toggleButton = card.querySelector(".sync-toggle");
    const intervalSelect = card.querySelector(".sync-interval");

    connectLink.addEventListener("click", function() {
        setStatus(card, "", "Waiting for provider authorization...");
    });

    toggleButton.addEventListener("click", async function() {
        const connection = currentConnection(providerId);
        if (!connection || !connection.athleteId) {
            return;
        }

        await persistSettings(card, providerId, connection.athleteId, {
            enabled: !connection.syncEnabled,
            interval: intervalSelect.value
        }, "The sync setting could not be updated. Try again.");
    });

    intervalSelect.addEventListener("change", async function() {
        const connection = currentConnection(providerId);
        if (!connection || !connection.athleteId) {
            return;
        }

        await persistSettings(card, providerId, connection.athleteId, {
            enabled: toggleButton.getAttribute("aria-checked") === "true",
            interval: intervalSelect.value
        }, "The collection interval could not be updated. Try again.");
    });
}

async function boot() {
    await processCallbackConnection();
    await syncStoredConnections();

    const connections = readStoredConnections();
    const cards = Array.from(document.querySelectorAll(".provider-card"));
    cards.forEach(function(card) {
        renderCard(card, connections[card.dataset.providerId] || null);
        bindCard(card);
    });
}

window.addEventListener("DOMContentLoaded", boot);
