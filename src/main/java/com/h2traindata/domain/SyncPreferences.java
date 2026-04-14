package com.h2traindata.domain;

public record SyncPreferences(boolean enabled, SyncInterval interval) {

    public SyncPreferences {
        interval = interval != null ? interval : SyncInterval.EVERY_24_HOURS;
    }

    public static SyncPreferences defaults() {
        return new SyncPreferences(true, SyncInterval.EVERY_24_HOURS);
    }
}
