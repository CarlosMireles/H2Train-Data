package com.h2traindata.web.mapper;

import com.h2traindata.domain.ProviderConnection;
import com.h2traindata.domain.SyncInterval;
import com.h2traindata.domain.SyncPreferences;
import com.h2traindata.web.dto.SyncSettingsRequest;
import com.h2traindata.web.dto.SyncSettingsResponse;
import org.springframework.stereotype.Component;

@Component
public class SyncSettingsMapper {

    public SyncSettingsResponse toResponse(ProviderConnection connection) {
        return new SyncSettingsResponse(
                connection.providerId(),
                connection.athlete().id(),
                connection.athlete().username(),
                true,
                connection.syncPreferences().enabled(),
                connection.syncPreferences().interval(),
                connection.syncPreferences().interval().label(),
                connection.lastSyncedAt()
        );
    }

    public SyncPreferences merge(ProviderConnection connection, SyncSettingsRequest request) {
        boolean enabled = request.enabled() != null
                ? request.enabled()
                : connection.syncPreferences().enabled();
        SyncInterval interval = request.interval() != null
                ? request.interval()
                : connection.syncPreferences().interval();
        return new SyncPreferences(enabled, interval);
    }
}
