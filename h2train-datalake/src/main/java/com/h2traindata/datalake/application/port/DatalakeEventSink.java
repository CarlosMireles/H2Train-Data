package com.h2traindata.datalake.application.port;

import com.h2traindata.datalake.domain.DatalakeEventRecord;
import java.nio.file.Path;

public interface DatalakeEventSink {

    Path write(DatalakeEventRecord eventRecord);
}
