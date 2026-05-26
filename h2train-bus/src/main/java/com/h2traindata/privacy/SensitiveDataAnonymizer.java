package com.h2traindata.privacy;

import com.h2traindata.domain.BusEventEnvelope;
import com.h2traindata.domain.EventPublication;
import com.h2traindata.domain.ProviderEvent;
import java.util.Map;

public interface SensitiveDataAnonymizer {

    Map<String, Object> anonymizeFields(Map<String, Object> fields);

    ProviderEvent anonymizeEvent(ProviderEvent event);

    EventPublication anonymizePublication(EventPublication publication);

    BusEventEnvelope anonymizeEnvelope(BusEventEnvelope envelope);
}
