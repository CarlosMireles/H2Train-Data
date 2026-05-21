package com.h2traindata.dataapp.application.port;

import com.h2traindata.domain.BusEventEnvelope;

public interface ProjectionHandler {

    boolean supports(BusEventEnvelope envelope);

    void project(BusEventEnvelope envelope);
}
