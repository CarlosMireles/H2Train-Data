package com.h2traindata.web.dto;

import com.h2traindata.domain.SyncInterval;

public record SyncSettingsRequest(
        Boolean enabled,
        SyncInterval interval
) {
}
