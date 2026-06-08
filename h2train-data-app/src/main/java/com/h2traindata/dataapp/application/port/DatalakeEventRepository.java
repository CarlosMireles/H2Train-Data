package com.h2traindata.dataapp.application.port;

import com.h2traindata.dataapp.domain.NormalizedDatalakeEvent;
import java.util.List;

public interface DatalakeEventRepository {

    List<NormalizedDatalakeEvent> readEvents();

    List<NormalizedDatalakeEvent> readEvents(String userId);
}
