package com.h2traindata.domain;

public enum EventType {
    USER_ACCOUNT(false, "user_account"),
    ACCOUNT_SYNC(false, "account_sync"),
    USER_STATE(false, "user_state"),
    ACTIVITY(true, "activity"),
    PHYSIOLOGICAL(false, "physiological"),
    BODY_COMPOSITION(false, "body_composition"),
    HEALTH(false, "health");

    private final boolean usesCursor;
    private final String resource;

    EventType(boolean usesCursor, String resource) {
        this.usesCursor = usesCursor;
        this.resource = resource;
    }

    public boolean usesCursor() {
        return usesCursor;
    }

    public String resource() {
        return resource;
    }
}
