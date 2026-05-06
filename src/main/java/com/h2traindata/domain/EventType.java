package com.h2traindata.domain;

public enum EventType {
    ACTIVITY(true),
    USER_METRICS(false);

    private final boolean usesCursor;

    EventType(boolean usesCursor) {
        this.usesCursor = usesCursor;
    }

    public boolean usesCursor() {
        return usesCursor;
    }
}
