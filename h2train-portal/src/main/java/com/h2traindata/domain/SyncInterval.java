package com.h2traindata.domain;

import java.time.Duration;

public enum SyncInterval {
    EVERY_5_HOURS(Duration.ofHours(5), "Every 5 hours"),
    EVERY_24_HOURS(Duration.ofHours(24), "Every 24 hours"),
    EVERY_7_DAYS(Duration.ofDays(7), "Every 7 days");

    private final Duration duration;
    private final String label;

    SyncInterval(Duration duration, String label) {
        this.duration = duration;
        this.label = label;
    }

    public Duration duration() {
        return duration;
    }

    public String label() {
        return label;
    }
}
