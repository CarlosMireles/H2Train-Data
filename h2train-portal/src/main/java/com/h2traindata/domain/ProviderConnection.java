package com.h2traindata.domain;

import java.time.Instant;

public record ProviderConnection(
        String providerId,
        AthleteProfile athlete,
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        SyncPreferences syncPreferences,
        SyncCursor lastSyncCursor,
        Instant lastSyncedAt,
        String userId
) {

    public ProviderConnection {
        syncPreferences = syncPreferences != null ? syncPreferences : SyncPreferences.defaults();
    }

    public ProviderConnection(String providerId,
                              AthleteProfile athlete,
                              String accessToken,
                              String refreshToken,
                              Instant expiresAt) {
        this(providerId, athlete, accessToken, refreshToken, expiresAt, SyncPreferences.defaults(), null, null, null);
    }

    public ProviderConnection(String providerId,
                              AthleteProfile athlete,
                              String accessToken,
                              String refreshToken,
                              Instant expiresAt,
                              SyncPreferences syncPreferences,
                              SyncCursor lastSyncCursor,
                              Instant lastSyncedAt) {
        this(providerId, athlete, accessToken, refreshToken, expiresAt, syncPreferences, lastSyncCursor, lastSyncedAt, null);
    }

    public ProviderConnection withTokens(String accessToken, String refreshToken, Instant expiresAt) {
        return new ProviderConnection(
                providerId,
                athlete,
                accessToken,
                refreshToken,
                expiresAt,
                syncPreferences,
                lastSyncCursor,
                lastSyncedAt,
                userId
        );
    }

    public ProviderConnection withSyncPreferences(SyncPreferences syncPreferences) {
        return new ProviderConnection(
                providerId,
                athlete,
                accessToken,
                refreshToken,
                expiresAt,
                syncPreferences,
                lastSyncCursor,
                lastSyncedAt,
                userId
        );
    }

    public ProviderConnection withSuccessfulSync(SyncCursor nextCursor, Instant syncedAt) {
        return new ProviderConnection(
                providerId,
                athlete,
                accessToken,
                refreshToken,
                expiresAt,
                syncPreferences,
                nextCursor != null ? nextCursor : lastSyncCursor,
                syncedAt,
                userId
        );
    }

    public ProviderConnection withUserId(String userId) {
        return new ProviderConnection(
                providerId,
                athlete,
                accessToken,
                refreshToken,
                expiresAt,
                syncPreferences,
                lastSyncCursor,
                lastSyncedAt,
                userId
        );
    }

    public boolean isSyncDue(Instant now) {
        if (!syncPreferences.enabled()) {
            return false;
        }
        if (lastSyncedAt == null) {
            return true;
        }
        return !lastSyncedAt.plus(syncPreferences.interval().duration()).isAfter(now);
    }
}
