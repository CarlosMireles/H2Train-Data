package com.h2traindata.dataapp.application.port;

import com.h2traindata.dataapp.domain.DataMartProjection;
import com.h2traindata.domain.EventType;
import java.util.List;
import java.util.Optional;

public interface DataMartRepository {

    void save(DataMartProjection projection);

    Optional<DataMartProjection> findByEventId(String providerId, String eventId);

    List<DataMartProjection> findByUserIdAndEventType(String userId, EventType eventType);
}
